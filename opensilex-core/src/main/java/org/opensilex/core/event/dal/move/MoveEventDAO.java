package org.opensilex.core.event.dal.move;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.geojson.Geometry;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.arq.querybuilder.Order;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.aggregate.Aggregator;
import org.apache.jena.sparql.expr.aggregate.AggregatorFactory;
import org.apache.jena.sparql.syntax.ElementFilter;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.vocabulary.RDF;
import org.bson.conversions.Bson;
import org.opensilex.core.event.dal.EventDAO;
import org.opensilex.core.geospatial.dal.GeospatialDAO;
import org.opensilex.core.ontology.Oeev;
import org.opensilex.core.ontology.Time;
import org.opensilex.core.organisation.dal.facility.FacilityModel;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializerNotFoundException;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.mapping.SPARQLClassObjectMapper;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLResult;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Stream;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Projections.excludeId;

public class MoveEventDAO extends EventDAO<MoveModel> {

    public static final String POSITION_ARRAY_FIELD = "targetPositions";
    public static final String TARGET_FIELD = "target";
    public static final String MOVE_COLLECTION_NAME = "move";

    public static final Var fromNameVar = SPARQLQueryHelper.makeVar(SPARQLClassObjectMapper.getObjectDefaultNameVarName(MoveModel.FROM_FIELD));
    public static final Var toNameVar = SPARQLQueryHelper.makeVar(SPARQLClassObjectMapper.getObjectDefaultNameVarName(MoveModel.TO_FIELD));
    private static final Var lastEndTimeStampVar = SPARQLQueryHelper.makeVar("last_end_ts");
    private static final Triple moveToTriple = new Triple(uriVar, Oeev.to.asNode(), toVar);
    private static final Triple moveTypeTriple = new Triple(uriVar, RDF.type.asNode(), Oeev.Move.asNode());
    private static final TriplePath lastEndTimeStampMatchingTriple = new TriplePath(new Triple(endInstantVar, Time.inXSDDateTimeStamp.asNode(), lastEndTimeStampVar));

    private final MongoCollection<MoveEventNoSqlModel> moveEventCollection;

    protected final static Logger LOGGER = LoggerFactory.getLogger(GeospatialDAO.class);

    public MoveEventDAO(SPARQLService sparql, MongoDBService mongodb) throws SPARQLException, SPARQLDeserializerNotFoundException {
        super(sparql, mongodb);
        this.moveEventCollection = mongodb.getDatabase().getCollection(MOVE_COLLECTION_NAME, MoveEventNoSqlModel.class);
    }

    public final MongoCollection<MoveEventNoSqlModel> getMoveEventCollection() {
        return moveEventCollection;
    }

    @Override
    public MoveModel create(MoveModel model) throws Exception {

        try {
            check(Collections.singletonList(model), true);

            sparql.startTransaction();
            sparql.create(eventGraph, model, false);

            // insert move event in mongodb
            MoveEventNoSqlModel noSqlModel = model.getNoSqlModel();
            if (noSqlModel != null) {

                String shortEventUri = URIDeserializer.getShortURI(model.getUri().toString());
                noSqlModel.setUri(new URI(shortEventUri));

                mongodb.startTransaction();
                moveEventCollection.insertOne(noSqlModel);
                mongodb.commitTransaction();
            }

            sparql.commitTransaction();
            return model;

        } catch (Exception e) {
            sparql.rollbackTransaction(e);
            mongodb.rollbackTransaction();
            throw e;
        }
    }

    protected void create(Collection<MoveModel> models, List<MoveEventNoSqlModel> noSqlModels) throws Exception {

        // insert sparql and noSql model streams
        try {
            sparql.startTransaction();
            sparql.createWithoutTransaction(eventGraph, models, SPARQLService.DEFAULT_MAX_INSTANCE_PER_QUERY, false, true);

            if (!noSqlModels.isEmpty()) {
                mongodb.startTransaction();
                moveEventCollection.insertMany(noSqlModels);
                mongodb.commitTransaction();
            }
            sparql.commitTransaction();

        } catch (Exception e) {
            sparql.rollbackTransaction(e);
            mongodb.rollbackTransaction();
            throw e;
        }
    }


    /**
     * @param models
     * @return
     * @throws Exception
     */
    @Override
    public List<MoveModel> create(List<MoveModel> models) throws Exception {

        List<MoveEventNoSqlModel> noSqlModels = new ArrayList<>();

        check(models, true);

        // build streams of sparql and no models from main model stream
        for (MoveModel model : models) {

            URI uri = model.getUri();
            if (uri == null) {
                uri = model.generateURI(eventGraph.toString(), model, 0);
                model.setUri(uri);
            }

            // set noSql model uri and update noSql model list
            MoveEventNoSqlModel noSqlModel = model.getNoSqlModel();
            if (noSqlModel != null) {
                String shortEventUri = URIDeserializer.getShortURI(model.getUri().toString());
                noSqlModel.setUri(new URI(shortEventUri));
                noSqlModels.add(noSqlModel);
            }

        }

        create(models, noSqlModels);
        return models;
    }

    /**
     * @param eventUri
     * @param user
     * @return
     * @throws Exception
     */
    public MoveModel getMoveEventByURI(URI eventUri, AccountModel user) throws Exception {

        MoveModel model = sparql.getByURI(MoveModel.class, eventUri, user.getLanguage());
        if (model == null) {
            return null;
        }
        MoveEventNoSqlModel noSqlModel = getMoveEventNoSqlModel(eventUri);
        if (noSqlModel != null) {
            model.setNoSqlModel(noSqlModel);
        }
        return model;
    }

    public List<MoveModel> getMoveEventByURIList(List<URI> uriList, AccountModel user) throws Exception {
        var modelList = sparql.getListByURIs(MoveModel.class, uriList, user.getLanguage());
        var noSqlModelMap = getMoveEventNoSqlModelMap(uriList);
        for (var model : modelList) {
            var noSqlModel = noSqlModelMap.get(SPARQLDeserializers.formatURI(model.getUri()));
            if (noSqlModel != null) {
                model.setNoSqlModel(noSqlModel);
            }
        }
        return modelList;
    }

    public MoveEventNoSqlModel getMoveEventNoSqlModel(URI uri) throws URISyntaxException {

        Objects.requireNonNull(uri);

        MoveEventNoSqlModel model = moveEventCollection
                .find(getEventIdFilter(uri))
                .projection(Projections.fields(excludeId()))
                .first();

        if (model == null) {
            return null;
        }

        model.setUri(uri);
        return model;
    }

    /**
     * Fetches a list of move NoSQL models by their URIs. The result is represented as a Map to facilitate the
     * integration with, for example, a list of {@link MoveModel}s. See for example
     * {@link #getMoveEventByURIList(List, AccountModel)}.
     *
     * @param uris The list of move URIs
     * @return A map associating the move URIs to their respective NoSQL models. Note that the map size can be smaller
     *         than the size of the URI list as moves do not always have an associated NoSQL model. The URI is formatted
     *         using {@link SPARQLDeserializers#formatURI(URI)}, so you SHOULD query this map using
     *         <code>map.get(SPARQLDeserializers.formatURI(uri))</code>.
     */
    public Map<URI, MoveEventNoSqlModel> getMoveEventNoSqlModelMap(List<URI> uris) {
        var iterableResult = moveEventCollection.find(getEventIdInFilter(uris.stream()));
        var resultMap = new HashMap<URI, MoveEventNoSqlModel>();
        for (var result : iterableResult) {
            resultMap.put(SPARQLDeserializers.formatURI(result.getUri()), result);
        }
        return resultMap;
    }

    /**
     * @param target the URI of the object concerned by the {@link MoveModel}
     * @return a {@link SPARQLResult} which the following bindings :
     */
    public MoveModel getLastMoveAfter(URI target, OffsetDateTime dateTime) throws Exception {

        ListWithPagination<MoveModel> moves = sparql.searchWithPagination(
                MoveModel.class,
                null,
                (select -> {
                    ElementGroup rootElementGroup = select.getWhereHandler().getClause();
                    ElementGroup eventGraphGroupElem = SPARQLQueryHelper.getSelectOrCreateGraphElementGroup(rootElementGroup, eventGraph);

                    appendTimeAfterFilter(eventGraphGroupElem, dateTime);
                    appendTargetEqFilter(eventGraphGroupElem, target.toString(), null);
                    select.addOrderBy(endInstantTimeStampVar, Order.DESCENDING);
                }),
                null,
                0,
                1
        );

        if (CollectionUtils.isEmpty(moves.getList())) {
            return null;
        }

        return moves.getList().get(0);
    }


    public MoveModel fromResult(SPARQLResult result, String lang, MoveModel model) throws Exception {

        super.fromResult(result, lang, model);

        String fromStr = result.getStringValue(MoveModel.FROM_FIELD);
        if (!StringUtils.isEmpty(fromStr)) {
            FacilityModel from = new FacilityModel();
            from.setUri(new URI(fromStr));
            from.setName(result.getStringValue(fromNameVar.getVarName()));
            model.setFrom(from);
        }

        String toStr = result.getStringValue(MoveModel.TO_FIELD);
        if (!StringUtils.isEmpty(toStr)) {
            FacilityModel to = new FacilityModel();
            to.setUri(new URI(toStr));
            to.setName(result.getStringValue(toNameVar.getVarName()));
            model.setTo(to);
        }

        return model;
    }


    public ListWithPagination<MoveModel> searchMoveEvents(
            URI target,
            String descriptionPattern,
            OffsetDateTime start, OffsetDateTime end,
            List<OrderBy> orderByList,
            Integer page, Integer pageSize) throws Exception {

        this.updateOrderByList(orderByList);

        return sparql.searchWithPagination(
                eventGraph,
                MoveModel.class,
                null,
                (select -> {
                    ElementGroup rootElementGroup = select.getWhereHandler().getClause();
                    ElementGroup eventGraphGroupElem = SPARQLQueryHelper.getSelectOrCreateGraphElementGroup(rootElementGroup, eventGraph);

                    // description is an optional field, so the filtering must be done outside of the OPTIONAL
                    appendDescriptionFilter(eventGraphGroupElem, descriptionPattern);

                    appendTargetEqFilter(eventGraphGroupElem, target.toString(), orderByList);
                    appendTimeFilter(eventGraphGroupElem, start, end);

                    if (CollectionUtils.isEmpty(orderByList)) {
                        select.addOrderBy(endInstantTimeStampVar, Order.DESCENDING);
                    }
                }),
                null,
                (result -> fromResult(result, null, new MoveModel())),
                orderByList,
                page,
                pageSize
        );
    }

    protected void appendTimeAfterFilter(ElementGroup eventGraphGroupElem, OffsetDateTime dateTime) throws Exception {
        if (dateTime != null) {
            Node dateTimeNode = SPARQLDeserializers.getForClass(OffsetDateTime.class).getNode(dateTime);
            Expr dateTimeExpr = SPARQLQueryHelper.getExprFactory().le(endInstantTimeStampVar, dateTimeNode);
            eventGraphGroupElem.addElementFilter(new ElementFilter(dateTimeExpr));
        }
    }

    @Override
    public MoveModel update(MoveModel model) throws Exception {

        check(Collections.singletonList(model), false);

        try {
            sparql.startTransaction();
            sparql.update(model);

            MoveEventNoSqlModel noSqlModel = model.getNoSqlModel();
            Bson idFilter = eq(MoveEventNoSqlModel.ID_FIELD, model.getUri());
            boolean moveExistInMongo = moveEventCollection.find(idFilter).first() != null;

            // the new move event has no data model in mongodb, so we need to delete the old if exists
            if (noSqlModel == null) {
                if (moveExistInMongo) {
                    mongodb.startTransaction();
                    moveEventCollection.deleteOne(idFilter);
                    mongodb.commitTransaction();
                }
            } else {
                noSqlModel.setUri(model.getUri());

                // insert or update the mongodb data model
                mongodb.startTransaction();
                if (moveExistInMongo) {
                    moveEventCollection.findOneAndReplace(idFilter, noSqlModel);
                } else {
                    moveEventCollection.insertOne(noSqlModel);
                }
                mongodb.commitTransaction();
            }

            sparql.commitTransaction();
        } catch (Exception e) {
            sparql.rollbackTransaction();
            mongodb.rollbackTransaction();
            throw e;
        }
        return model;
    }

    @Override
    public void delete(URI uri) throws Exception {

        try {
            sparql.startTransaction();
            sparql.delete(MoveModel.class, uri);

            // first check if the model exist
            boolean moveExistInMongo = mongodb.uriExists(moveEventCollection, uri, MoveEventNoSqlModel.ID_FIELD);

            // start the transaction for deleting, only if the model exist
            if (moveExistInMongo) {
                mongodb.startTransaction();
                moveEventCollection.deleteOne(eq(MoveEventNoSqlModel.ID_FIELD, uri));
                mongodb.commitTransaction();
            }

            sparql.commitTransaction();

        } catch (Exception e) {
            sparql.rollbackTransaction();
            mongodb.rollbackTransaction();
            throw e;
        }

    }

    /**
     * @param target the object on which we get the position
     * @param start  the time at which we search the position
     * @return the position of the given object during a time interval with a descending sort on the move end.
     * @apiNote The method run in two times :
     * <ul>
     * <li> Search corresponding move URI and location from the SPARQL repository </li>
     * <li> Then for each move URI, get the corresponding {@link MoveEventNoSqlModel} from the mongodb collection.
     * <ul>
     * <li> This last operation is done with a single query with a IN filter on {@link MoveEventNoSqlModel#ID_FIELD} field. </li>
     * <li> A {@link Map} between move URI and {@link PositionModel} is maintained in order to associated data from the two databases</li>
     * </ul>
     * </li>
     * </ul>
     * @see MoveEventDAO#searchMoveEvents(URI, String, OffsetDateTime, OffsetDateTime, List, Integer, Integer)
     */
    public ListWithPagination<MoveModel> getPositionsHistory(
            URI target,
            String descriptionPattern,
            OffsetDateTime start,
            OffsetDateTime end,
            List<OrderBy> orderByList,
            Integer page, Integer pageSize) throws Exception {

        Objects.requireNonNull(target);

        // search move history sorted with DESC order on move end, from SPARQL repository
        ListWithPagination<MoveModel> locationHistory = searchMoveEvents(target, descriptionPattern, start, end, orderByList, page, pageSize);

        // Index of position by uri, sorted by event end time
        LinkedHashMap<URI, PositionModel> positionsByUri = new LinkedHashMap<>();
        Map<URI, MoveModel> moveByURI = new HashMap<>();

        // for each location, create a position model initialized with the location and update position index
        locationHistory.forEach(move -> {
            moveByURI.put(move.getUri(), move);
            positionsByUri.put(move.getUri(), null);
        });


        if (start != null) {
            MoveModel lastMove = getLastMoveAfter(target, start);
            if (lastMove != null) {
                positionsByUri.put(lastMove.getUri(), null);
            }
        }

        if (!positionsByUri.isEmpty()) {

            Bson eventInIdFilter = getEventIdInFilter(positionsByUri.keySet().stream());
            Bson concernedItemPositionProjection = getConcernedItemArrayItemProjection(target);

            // found all positions from mongodb collection according the filter
            moveEventCollection
                    .find(eventInIdFilter)
                    .skip(page * pageSize)
                    .limit(pageSize)
                    .projection(concernedItemPositionProjection)//  don't fetch concernedItem and position of other item
                    .forEach(moveNoSqlModel -> {
                        if (moveNoSqlModel.getTargetPositions().size() > 0) {
                            positionsByUri.put(moveNoSqlModel.getUri(), moveNoSqlModel.getTargetPositions().get(0).getPosition());
                        } else {
                            positionsByUri.remove(moveNoSqlModel.getUri());
                        }
                    });

        }

        locationHistory.forEach(move -> {
            var targetPosition = new TargetPositionModel();
            targetPosition.setTarget(target);
            targetPosition.setPosition(positionsByUri.get(move.getUri()));
            var noSqlModel = new MoveEventNoSqlModel();
            noSqlModel.setTargetPositions(Collections.singletonList(targetPosition));
            move.setNoSqlModel(noSqlModel);
        });
        return locationHistory;
    }

    /**
     * @param concernedItemUri the URI of the item concerned by a move event.
     * @return a {@link Bson} with {@link Filters#eq(Object)} expression on itemPositions.concernedItem property and the given URI
     * @see MoveEventNoSqlModel#getTargetPositions()
     * @see TargetPositionModel#getPosition()
     */
    private Bson getConcernedItemArrayItemProjection(URI concernedItemUri) {
        return Filters.elemMatch(POSITION_ARRAY_FIELD,
                Filters.eq(TARGET_FIELD, concernedItemUri)
        );
    }

    /**
     * @param eventUris the {@link Stream} of event URI
     * @return a {@link Bson} with a {@link Filters#in(String, Object[])} with {@link MoveEventNoSqlModel#ID_FIELD} and the given URI stream
     */
    private Bson getEventIdInFilter(Stream<URI> eventUris) {

        return Filters.in(MoveEventNoSqlModel.ID_FIELD, eventUris.map(uri -> {
            try {
                return new URI(URIDeserializer.getShortURI(uri.toString()));
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        })::iterator);
    }

    /**
     * @param uri the event uri
     * @return a {@link Bson} with {@link Filters#eq(Object)} expression between {@link MoveEventNoSqlModel#ID_FIELD} and the prefixed version of the
     * given uri
     * @throws URISyntaxException when trying to create a prefixed version of the given uri
     * @apiNote the method will search on collection with a prefixed version of the given uri as id
     */
    private static Bson getEventIdFilter(URI uri) throws URISyntaxException {
        Objects.requireNonNull(uri);
        URI shortUri = new URI(URIDeserializer.getShortURI(uri.toString()));
        return Filters.eq(MoveEventNoSqlModel.ID_FIELD, shortUri);
    }

    /**
     * @param concernedItem the object on which we get the last move event
     * @return the last move event of the given object, null if no move event was found.
     */
    public MoveModel getLastMoveEvent(URI concernedItem) throws Exception {
        return this.getLastMoveAfter(concernedItem, null);
    }

    public Map<URI, URI> getLastLocations(Stream<URI> targets, int size) throws Exception {

        // use the MAX aggregator in order to compute the last (the highest considering a lexicographic order) end timestamp
        Aggregator maxEndTsAggregator = AggregatorFactory.createMax(false, SPARQLQueryHelper.getExprFactory().asExpr(endInstantTimeStampVar));

        // select for each target, the end timestamp of the last move event concerning the target
        // use inner select in order to have one timestamp per target
        SelectBuilder innerSelect = new SelectBuilder()
                .addVar(targetVar)
                .addVar(maxEndTsAggregator.asSparqlExpr(null), lastEndTimeStampVar)
                .addGraph(eventGraph, new WhereBuilder()
                        .addWhere(targetTriple)
                        .addWhere(moveTypeTriple)
                        .addWhere(moveToTriple)
                        .addWhere(endTriple)
                        .addWhere(endInstantTimeStampTriple)
                ).addGroupBy(targetVar);

        // append VALUES on ?targets
        SPARQLQueryHelper.addWhereUriValues(innerSelect, targetVar.getVarName(), targets, size);

        SelectBuilder outerSelect = new SelectBuilder()
                .addVar(targetVar)
                .addVar(toVar)
                .addGraph(eventGraph, new WhereBuilder()
                        .addWhere(lastEndTimeStampMatchingTriple)  // match with inner ?last_end_ts
                        .addWhere(targetTriple) // match with inner ?targets
                        .addWhere(endTriple)
                        .addWhere(moveToTriple)
                        .addSubQuery(innerSelect)
                );

        Map<URI, URI> targetLastLocations = new HashMap<>();

        sparql.executeSelectQueryAsStream(outerSelect).forEach(
            result -> {
                String lastLocation = result.getStringValue(MoveModel.TO_FIELD);
                if (lastLocation != null) {
                    String target = result.getStringValue(MoveModel.TARGETS_FIELD);
                    if (target != null) {
                        targetLastLocations.put(URIDeserializer.formatURI(target), URIDeserializer.formatURI(lastLocation));
                    }
                }
        });
        return targetLastLocations;

    }


    /**
     * @param objectUri the object on which we get the position
     * @param moveURI   the associated move event uri
     * @return the position of the given object at a given time, null if no move event was found.
     * @apiNote if time is null, then the last known position will be returned
     */
    public PositionModel getPosition(URI objectUri, URI moveURI) throws Exception {

        // fetch event uri and location from SPARQL result
        Bson eventIdFilter = getEventIdFilter(moveURI);
        Bson concernedItemPositionProjection = getConcernedItemArrayItemProjection(objectUri);

        MoveEventNoSqlModel moveNoSqlModel = moveEventCollection
                .find(eventIdFilter)
                .projection(
                        Projections.fields(
                                excludeId(), // don't fetch position _id field
                                concernedItemPositionProjection //  don't fetch concernedItem and position of other item
                        )
                )
                .first();

        if (moveNoSqlModel == null || moveNoSqlModel.getTargetPositions().size() == 0) {
            return null;
        }

        return moveNoSqlModel.getTargetPositions().get(0).getPosition();
    }

   public FindIterable<MoveEventNoSqlModel> getIntersectPosition(List<MoveEventNoSqlModel> moveEventNoSqlModelList, Geometry geometry){

        Bson inFilter = getEventIdInFilter(moveEventNoSqlModelList.stream().map(MoveEventNoSqlModel::getUri));

        Bson query = and(Filters.or(inFilter),Filters.exists(MoveEventNoSqlModel.COORDINATES_FIELD, true), Filters.geoWithin(MoveEventNoSqlModel.COORDINATES_FIELD, geometry));
        LOGGER.debug("MongoDB search intersect:{}", query);

        return moveEventCollection.find(query);
    }


    /**
     * Count total of moves associated to a target URI
     * 
     * @param target the URI on which find associated move
     * @return the number of moves associated to a target
     */
    public int countMoves(URI target) throws Exception {

        Node moveGraph = sparql.getDefaultGraph(MoveModel.class);
        return sparql.count(moveGraph, MoveModel.class,null,countBuilder -> {
            super.appendInTargetsValuesInner(countBuilder, Stream.of(target), 1, moveGraph);
        },null);
    }
}

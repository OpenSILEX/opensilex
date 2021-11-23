package org.opensilex.core.event.dal.move;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.arq.querybuilder.Order;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.arq.querybuilder.handlers.WhereHandler;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.aggregate.Aggregator;
import org.apache.jena.sparql.expr.aggregate.AggregatorFactory;
import org.apache.jena.sparql.syntax.ElementFilter;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.ElementNamedGraph;
import org.apache.jena.vocabulary.RDF;
import org.bson.conversions.Bson;
import org.opensilex.core.device.dal.DeviceModel;
import org.opensilex.core.event.dal.EventDAO;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.ontology.Oeev;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.ontology.Time;
import org.opensilex.core.organisation.dal.InfrastructureFacilityModel;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.authentication.SecurityOntology;
import org.opensilex.security.group.dal.GroupUserProfileModel;
import org.opensilex.security.profile.dal.ProfileModel;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializerNotFoundException;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.mapping.SPARQLClassObjectMapper;
import org.opensilex.sparql.model.SPARQLTreeModel;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLResult;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.Ontology;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;

import javax.validation.constraints.NotNull;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Stream;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Projections.excludeId;
import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;

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
            sparql.create(eventGraph, models, SPARQLService.DEFAULT_MAX_INSTANCE_PER_QUERY, false);

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
    public MoveModel getMoveEventByURI(URI eventUri, UserModel user) throws Exception {

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
            InfrastructureFacilityModel from = new InfrastructureFacilityModel();
            from.setUri(new URI(fromStr));
            from.setName(result.getStringValue(fromNameVar.getVarName()));
            model.setFrom(from);
        }

        String toStr = result.getStringValue(MoveModel.TO_FIELD);
        if (!StringUtils.isEmpty(toStr)) {
            InfrastructureFacilityModel to = new InfrastructureFacilityModel();
            to.setUri(new URI(toStr));
            to.setName(result.getStringValue(toNameVar.getVarName()));
            model.setTo(to);
        }

        return model;
    }


    public Stream<MoveModel> searchMoveEvents(
            URI target,
            String descriptionPattern,
            OffsetDateTime start, OffsetDateTime end,
            List<OrderBy> orderByList,
            Integer page, Integer pageSize) throws Exception {

        this.updateOrderByList(orderByList);

        return sparql.searchAsStream(
                eventGraph,
                MoveModel.class,
                null,
                (select -> {
                    ElementGroup rootElementGroup = select.getWhereHandler().getClause();
                    ElementGroup eventGraphGroupElem = SPARQLQueryHelper.getSelectOrCreateGraphElementGroup(rootElementGroup, eventGraph);

                    // description is an optional field, so the filtering must be done outside of the OPTIONAL
                    appendDescriptionFilter(eventGraphGroupElem, descriptionPattern);

                    appendTargetEqFilter(eventGraphGroupElem, target.toString(), orderByList);
                    appendTimeFilter(select,eventGraphGroupElem, start, end);

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
    public LinkedHashMap<MoveModel, PositionModel> getPositionsHistory(
            @NotNull URI target,
            String descriptionPattern,
            OffsetDateTime start, OffsetDateTime end,
            List<OrderBy> orderByList,
            @NotNull Integer page, @NotNull Integer pageSize) throws Exception {

        Objects.requireNonNull(target);

        // search move history sorted with DESC order on move end, from SPARQL repository
        Stream<MoveModel> locationHistory = searchMoveEvents(target, descriptionPattern, start, end, orderByList, page, pageSize);

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

        LinkedHashMap<MoveModel, PositionModel> results = new LinkedHashMap<>();
        positionsByUri.forEach((uri, position) -> {
            results.put(moveByURI.get(uri), position);
        });
        return results;
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
    public LinkedHashMap<MoveModel, PositionModel> getPositionsHistoryWithoutPagination(
            @NotNull URI target,
            String descriptionPattern,
            OffsetDateTime start, OffsetDateTime end) throws Exception {

        Objects.requireNonNull(target);
//        OrderBy orderBy = new OrderBy("_end__timestamp=asc");
        OrderBy orderBy = new OrderBy("_end__timestamp", Order.ASCENDING);
        ArrayList<OrderBy> arr_OrderBy = new ArrayList<OrderBy>();
        arr_OrderBy.add(orderBy);
        // search move history sorted with DESC order on move end, from SPARQL repository
        Stream<MoveModel> locationHistory = searchMoveEvents(target, descriptionPattern, start, end, arr_OrderBy, null, null);

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
                    .projection(concernedItemPositionProjection)//  don't fetch concernedItem and position of other item
                    .forEach(moveNoSqlModel -> {
                        if (moveNoSqlModel.getTargetPositions().size() > 0) {
                            positionsByUri.put(moveNoSqlModel.getUri(), moveNoSqlModel.getTargetPositions().get(0).getPosition());
                        } else {
                            positionsByUri.remove(moveNoSqlModel.getUri());
                        }
                    });

        }

        LinkedHashMap<MoveModel, PositionModel> results = new LinkedHashMap<>();
        positionsByUri.forEach((uri, position) -> {
            results.put(moveByURI.get(uri), position);
        });
        return results;
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


    public enum ReverseLink {FROM, TO} ;
    /**
     * @param link The reverse search direction
     * @param targetUri the object to reverse search
     * @return A list of all device model which are a target of the event
     * @apiNote if time is null, then the last known position will be returned
     */
    public List<URI> reverseSearchURIDevice(URI targetUri, ReverseLink link ) throws Exception {
        // select for each target, the devices attached to it via a move event
        Var device_var = makeVar("device");
        Var event_var = makeVar("move");
        Var type_var = makeVar("type");
        SelectBuilder innerSelect = new SelectBuilder()
                .addVar(device_var)
                .addWhere(event_var, Oeev.to, SPARQLDeserializers.nodeURI(targetUri))
                .addWhere(event_var, Oeev.concerns, device_var)
                .addWhere(device_var, RDF.type, type_var)
                .addWhere(type_var, Ontology.subClassAny, Oeso.Device);

        ArrayList<URI> devicesURIs = new ArrayList<URI>();
        sparql.executeSelectQueryAsStream(innerSelect).forEach(result -> {
            String device_uri = result.getStringValue(device_var.getVarName());
            try {
                devicesURIs.add(new URI(device_uri));
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        });
        return devicesURIs;
    }
}

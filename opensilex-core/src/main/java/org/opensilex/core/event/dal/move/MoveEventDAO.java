package org.opensilex.core.event.dal.move;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.Order;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.syntax.ElementFilter;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.vocabulary.RDF;
import org.bson.conversions.Bson;
import org.opensilex.core.event.dal.EventDAO;
import org.opensilex.core.ontology.Oeev;
import org.opensilex.core.ontology.dal.ClassModel;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.user.dal.UserModel;
import org.opensilex.sparql.deserializer.DateTimeDeserializer;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLResult;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Projections.excludeId;

public class MoveEventDAO extends EventDAO<MoveModel> {
    
    public final static String POSITION_ARRAY_FIELD = "targetPositions";
    public final static String TARGET_FIELD = "target";
    
    public final static String moveCollectionName = "Moves";
    private final MongoCollection<MoveEventNoSqlModel> moveEventCollection;
    
    private static final Triple moveEventTo = new Triple(uriVar, Oeev.from.asNode(), fromVar);
    private static final Triple moveEventFrom = new Triple(uriVar, Oeev.to.asNode(), toVar);
    private static final Triple moveEventType = new Triple(uriVar, RDF.type.asNode(), Oeev.Move.asNode());
    
    public MoveEventDAO(SPARQLService sparql, MongoDBService mongodb) throws SPARQLException {
        super(sparql, mongodb);
        this.moveEventCollection = mongodb.getDatabase().getCollection(moveCollectionName, MoveEventNoSqlModel.class);
    }
    
    public final MongoCollection<MoveEventNoSqlModel> getMoveEventCollection() {
        return moveEventCollection;
    }

    public MoveModel create(MoveModel model) throws Exception {
        
        try {
            check(Collections.singletonList(model),true);
            
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
    
    protected void createMoveEvents(Collection<MoveModel> models, List<MoveEventNoSqlModel> noSqlModels) throws Exception {

        // insert sparql and noSql model streams
        try {
            sparql.startTransaction();
            sparql.create(eventGraph, models, SPARQLService.DEFAULT_MAX_INSTANCE_PER_QUERY, false);
            
            if (! noSqlModels.isEmpty()) {
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
    public List<MoveModel> create(List<MoveModel> models) throws Exception {

        List<MoveEventNoSqlModel> noSqlModels = new ArrayList<>();

        check(models,true);

        // build streams of sparql and no models from main model stream
        for(MoveModel model : models){

            URI uri = model.getUri();
            if(uri == null){
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

        createMoveEvents(models, noSqlModels);
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
    public MoveModel getMoveEvent(URI target, OffsetDateTime dateTime) throws Exception {
        
        ListWithPagination<MoveModel> moves = sparql.searchWithPagination(
                MoveModel.class,
                null,
                (select -> {
                    ElementGroup rootElementGroup = select.getWhereHandler().getClause();
                    ElementGroup eventGraphGroupElem = SPARQLQueryHelper.getSelectOrCreateGraphElementGroup(rootElementGroup, eventGraph);
                    
                    appendTargetFilter(eventGraphGroupElem, target);
                    appendTimeAfterFilter(eventGraphGroupElem, dateTime);
                    
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

    /**
     * @param concernedItem the URI of the object concerned by the {@link MoveModel}
     * @param start the time interval start
     * @param end the time interval end
     * @return a {@link Stream} of {@link MoveModel} which the following bindings :
     * @apiNote The {@link MoveModel} are sorted with DESCENDING ordering on event end.
     */
    public Stream<MoveModel> searchMoveEvents(
            URI concernedItem,
            String descriptionPattern,
            OffsetDateTime start, OffsetDateTime end,
            List<OrderBy> orderByList,
            Integer page, Integer pageSize) throws Exception {
        
        int offset = 0;
        int limit = 20;
        if (page != null && pageSize != null) {
            offset = page * pageSize;
            limit = pageSize;
        }

        // build the index of couple (Expr,order) for any specific order by
        Map<Expr, Order> exprOrderIndex = new HashMap<>();

        // build the index of default order by indexed by field name
        Map<String, OrderBy> defaultOrderIndex = orderByList.stream()
                .collect(Collectors.toMap(OrderBy::getFieldName, order -> order));
        
        return sparql.searchAsStream(
                eventGraph,
                MoveModel.class,
                null,
                (select -> {
                    ElementGroup rootElementGroup = select.getWhereHandler().getClause();
                    ElementGroup eventGraphGroupElem = SPARQLQueryHelper.getSelectOrCreateGraphElementGroup(rootElementGroup, eventGraph);
                    
                    appendDescriptionFilter(eventGraphGroupElem, descriptionPattern);
                    appendTargetFilter(eventGraphGroupElem, concernedItem);
                    appendTimeFilter(eventGraphGroupElem, start, end);

//                  appendDateIntervalRangeFilter(eventGraphGroupElem, start, end);
                    if (CollectionUtils.isEmpty(orderByList)) {
                        select.addOrderBy(endInstantTimeStampVar, Order.DESCENDING);
                    } else {
                        exprOrderIndex.forEach(select::addOrderBy);
                    }
                }),
                defaultOrderIndex.values(),
                offset,
                limit
        );
    }
    
    protected void appendTimeAfterFilter(ElementGroup eventGraphGroupElem, OffsetDateTime dateTime) throws Exception {
        if (dateTime != null) {
            Node dateTimeNode = SPARQLDeserializers.getForClass(OffsetDateTime.class).getNode(dateTime);
            Expr dateTimeExpr = SPARQLQueryHelper.getExprFactory().le(endInstantTimeStampVar, dateTimeNode);
            eventGraphGroupElem.addElementFilter(new ElementFilter(dateTimeExpr));
        }
    }
    
    protected void appendDateIntervalRangeFilter(ElementGroup eventGraphGroupElem, OffsetDateTime start, OffsetDateTime end) throws Exception {
        
        boolean beginTimeNotNull = start != null;
        boolean endTimeNotNull = end != null;
        
        if (!beginTimeNotNull && !endTimeNotNull) {
            return;
        }
        
        ExprFactory exprFactory = SPARQLQueryHelper.getExprFactory();
        Expr beginTimeGeThanEffectiveTimeExpr = null;
        Expr endTimeLeThanEffectiveTimeExpr = null;
        DateTimeDeserializer dateTimeDeserializer = new DateTimeDeserializer();
        
        if (beginTimeNotNull) {
            Node beginTimeNode = dateTimeDeserializer.getNode(start);
            beginTimeGeThanEffectiveTimeExpr = exprFactory.ge(endInstantTimeStampVar, beginTimeNode);
        }
        if (endTimeNotNull) {
            Node endTimeNode = dateTimeDeserializer.getNode(end);
            endTimeLeThanEffectiveTimeExpr = exprFactory.le(endInstantTimeStampVar, endTimeNode);
        }
        
        Expr dateIntervalExpr;
        if (beginTimeNotNull && endTimeNotNull) {
            dateIntervalExpr = exprFactory.and(beginTimeGeThanEffectiveTimeExpr, endTimeLeThanEffectiveTimeExpr);
        } else if (beginTimeNotNull) {
            dateIntervalExpr = beginTimeGeThanEffectiveTimeExpr;
        } else {
            dateIntervalExpr = endTimeLeThanEffectiveTimeExpr;
        }
        
        eventGraphGroupElem.addElementFilter(new ElementFilter(dateIntervalExpr));
    }

    @Override
    public MoveModel update(MoveModel model, ClassModel classModel) throws Exception {

        check(Collections.singletonList(model),false);

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
     * @param concernedItem the object on which we get the position
     * @param start the time at which we search the position
     * @return the position of the given object during a time interval with a descending sort on the move end.
     *
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
     *
     *
     * @see MoveEventDAO#searchMoveEvents(URI, String, OffsetDateTime, OffsetDateTime, List, Integer, Integer)
     */
    public LinkedHashMap<MoveModel, PositionModel> getPositionsHistory(URI concernedItem,
            String descriptionPattern,
            OffsetDateTime start, OffsetDateTime end,
            List<OrderBy> orderByList,
            Integer page, Integer pageSize) throws Exception {
        
        Objects.requireNonNull(concernedItem);

        // Index of position by uri, sorted by event end time
        LinkedHashMap<URI, PositionModel> positionList = new LinkedHashMap<>();

        // search move history sorted with DESC order on move end, from SPARQL repository
        Stream<MoveModel> locationHistory = searchMoveEvents(concernedItem, descriptionPattern, start, end, orderByList, page, pageSize);
        
        Map<URI, MoveModel> moveByURI = new HashMap<>();
        // for each location, create a position model initialized with the location and update position index
        locationHistory.forEach(result -> {
            moveByURI.put(result.getUri(), result);
            positionList.put(result.getUri(), null);
        });
        
        if (start != null) {
            MoveModel lastMove = getMoveEvent(concernedItem, start);
            if (lastMove != null) {
                positionList.put(lastMove.getUri(), null);
            }
        }
        
        if (!positionList.isEmpty()) {
            
            Bson eventInIdFilter = getEventIdInFilter(positionList.keySet().stream());
            Bson concernedItemPositionProjection = getConcernedItemArrayItemProjection(concernedItem);

            // found all positions from mongodb collection according the filter
            moveEventCollection
                    .find(eventInIdFilter)
                    .skip(page * pageSize)
                    .limit(pageSize)
                    .projection(concernedItemPositionProjection)//  don't fetch concernedItem and position of other item
                    .forEach(moveNoSqlModel -> {
                        if (moveNoSqlModel.getTargetPositions().size() > 0) {
                            positionList.put(moveNoSqlModel.getUri(), moveNoSqlModel.getTargetPositions().get(0).getPosition());
                        } else {
                            positionList.remove(moveNoSqlModel.getUri());
                        }
                    });
            
        }
        
        LinkedHashMap<MoveModel, PositionModel> result = new LinkedHashMap<>();
        positionList.forEach((uri, position) -> {
            result.put(moveByURI.get(uri), position);
        });
        return result;
    }

    /**
     *
     * @param concernedItemUri the URI of the item concerned by a move event.
     * @return a {@link Bson} with {@link Filters#eq(Object)} expression on itemPositions.concernedItem property and the given URI
     *
     * @see MoveEventNoSqlModel#getTargetPositions()
     * @see TargetPositionModel#getPosition()
     */
    private Bson getConcernedItemArrayItemProjection(URI concernedItemUri) {
        return Filters.elemMatch(POSITION_ARRAY_FIELD,
                Filters.eq(TARGET_FIELD, concernedItemUri)
        );
    }

    /**
     *
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
     *
     * @param uri the event uri
     * @return a {@link Bson} with {@link Filters#eq(Object)} expression between {@link MoveEventNoSqlModel#ID_FIELD} and the prefixed version of the
     * given uri
     * @throws URISyntaxException when trying to create a prefixed version of the given uri
     * @apiNote the method will search on collection with a prefixed version of the given uri as id
     *
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
        return this.getMoveEvent(concernedItem, null);
    }

    /**
     * @param objectUri the object on which we get the position
     * @param moveURI the associated move event uri
     * @return the position of the given object at a given time, null if no move event was found.
     *
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
}

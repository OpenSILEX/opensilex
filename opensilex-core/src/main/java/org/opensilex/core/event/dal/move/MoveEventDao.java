package org.opensilex.core.event.dal.move;

import com.mongodb.client.MongoCollection;
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
import org.opensilex.core.event.dal.EventDao;
import org.opensilex.core.ontology.Oeev;
import org.opensilex.core.position.dal.PositionDao;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Projections.excludeId;

public class MoveEventDao extends EventDao {

    public final static String moveCollectionName = "Moves";
    private final MongoCollection<MoveEventNoSqlModel> moveEventCollection;

    private static final Triple moveEventTo;
    private static final Triple moveEventFrom;
    private static final Triple moveEventType;

    static {
        moveEventFrom = new Triple(uriVar, Oeev.from.asNode(), fromVar);
        moveEventTo = new Triple(uriVar, Oeev.to.asNode(), toVar);
        moveEventType = new Triple(uriVar, RDF.type.asNode(), Oeev.Move.asNode());
    }

    public MoveEventDao(SPARQLService sparql, MongoDBService mongodb) throws SPARQLException {
        super(sparql, mongodb);
        this.moveEventCollection = mongodb.getDatabase().getCollection(moveCollectionName, MoveEventNoSqlModel.class);
    }

    public final MongoCollection<MoveEventNoSqlModel> getMoveEventCollection() {
        return moveEventCollection;
    }

    private void check(MoveModel move) throws Exception {

        if (!sparql.uriListExists(null, move.getConcernedItems())) {
            throw new IllegalArgumentException("One URI from concerned items was not found");
        }
    }

    private void checkAll(List<MoveModel> models) throws Exception {

        final int batchSize = 256;
        int i = 0;

        List<URI> concernedItemUris = new ArrayList<>(batchSize);
        List<URI> uris = new ArrayList<>(batchSize);

        for(MoveModel model : models){

            URI uri = model.getUri();
            if(uri != null){

                if (model.getUri().toString().isEmpty()) {
                    throw new IllegalArgumentException("Empty model uri at index " + 0);
                }

                uris.add(model.getUri());
                if(uris.size() >= batchSize || i == models.size()-1){
                    if (sparql.anyUriExists(null, uris)) {
                        throw new IllegalArgumentException("One move URI already exist");
                    }
                    uris.clear();
                }

                concernedItemUris.addAll(model.getConcernedItems());
                if(concernedItemUris.size() >= batchSize || i == models.size()-1){
                    if (!sparql.uriListExists(null, concernedItemUris)) {
                        throw new IllegalArgumentException("One URI from concerned items was not found");
                    }
                    concernedItemUris.clear();
                }

                // #TODO add checking on FROM/TO
            }else{
                uri = model.generateURI(eventGraph.toString(), model, 0);
                model.setUri(uri);
            }

            MoveEventNoSqlModel noSqlModel = model.getNoSqlModel();
            if(noSqlModel != null){
                String shortEventUri = URIDeserializer.getShortURI(model.getUri().toString());
                noSqlModel.setUri(new URI(shortEventUri));
            }

        };

    }

    public MoveModel create(MoveModel model) throws Exception {

        try {
            check(model);

            sparql.startTransaction();
            sparql.create(eventGraph, model, false);

            // insert move event in mongodb
            MoveEventNoSqlModel noSqlModel = model.getNoSqlModel();
            if (noSqlModel != null) {
                noSqlModel.setUri(new URI(URIDeserializer.getShortURI(model.getUri().toString())));

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


    protected void createMoveEvents(Collection<MoveModel> models, List<MoveEventNoSqlModel> noSqlModels, boolean anyMoveNoSqlModelFound) throws Exception {

        // insert sparql and noSql model streams
        try {
            sparql.startTransaction();
            sparql.create(eventGraph,models,SPARQLService.DEFAULT_MAX_INSTANCE_PER_QUERY,false);

            if (anyMoveNoSqlModelFound) {
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
    public void createMoveEvents(List<MoveModel> models) throws Exception {

        Stream.Builder<MoveModel> checkModelStream = Stream.builder();

        List<MoveEventNoSqlModel> noSqlModels = new ArrayList<>(models.size());
        AtomicBoolean anyMoveNoSqlModelFound = new AtomicBoolean(false);

        // build streams of sparql and no models from main model stream
        models.forEach(model -> {
            checkModelStream.add(model);
            try {
                // set noSql model uri and update noSql model stream
                MoveEventNoSqlModel noSqlModel = model.getNoSqlModel();
                if (noSqlModel != null) {
                    noSqlModels.add(noSqlModel);
                    anyMoveNoSqlModelFound.set(true);
                }

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        checkAll(models);
        createMoveEvents(models,noSqlModels, anyMoveNoSqlModelFound.get());
    }

    /**
     * @param eventUri
     * @param user
     * @return
     * @throws Exception
     */
    public MoveModel getMoveEvent(URI eventUri, UserModel user) throws Exception {

        MoveModel model = sparql.getByURI(MoveModel.class, eventUri, user.getLanguage());
        if (model == null) {
            return null;
        }
        MoveEventNoSqlModel noSqlModel = getMoveEventNoSqlModel(eventUri);
        if(noSqlModel != null){
            model.setNoSqlModel(noSqlModel);
        }
        return model;
    }

    public MoveEventNoSqlModel getMoveEventNoSqlModel(URI uri) throws URISyntaxException {

        Objects.requireNonNull(uri);

        MoveEventNoSqlModel model = moveEventCollection
                .find(PositionDao.getEventIdFilter(uri))
                .projection(Projections.fields(excludeId()))
                .first();

        if (model == null) {
            return null;
        }

        model.setUri(uri);
        return model;
    }

    /**
     * @param concernedItem the URI of the object concerned by the {@link MoveModel}
     * @return a {@link SPARQLResult} which the following bindings :
     */
    public MoveModel getMoveEvent(URI concernedItem, OffsetDateTime dateTime) throws Exception {

        ListWithPagination<MoveModel> moves = sparql.searchWithPagination(
                MoveModel.class,
                null,
                (select -> {
                    ElementGroup rootElementGroup = select.getWhereHandler().getClause();
                    ElementGroup eventGraphGroupElem = SPARQLQueryHelper.getSelectOrCreateGraphElementGroup(rootElementGroup, eventGraph);

                    appendConcernedItemFilter(eventGraphGroupElem, concernedItem);
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
     * @param start         the time interval start
     * @param end           the time interval end
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
        Map<String,OrderBy> defaultOrderIndex =  orderByList.stream()
                .collect(Collectors.toMap(OrderBy::getFieldName, order -> order));

        return sparql.searchAsStream(
                eventGraph,
                MoveModel.class,
                null,
                (select -> {
                    ElementGroup rootElementGroup = select.getWhereHandler().getClause();
                    ElementGroup eventGraphGroupElem = SPARQLQueryHelper.getSelectOrCreateGraphElementGroup(rootElementGroup, eventGraph);

                    appendDescriptionFilter(eventGraphGroupElem, descriptionPattern);
                    appendConcernedItemFilter(eventGraphGroupElem, concernedItem);
                    appendTimeFilterAndOrder(eventGraphGroupElem, start, end, exprOrderIndex, defaultOrderIndex);

//                  appendDateIntervalRangeFilter(eventGraphGroupElem, start, end);

                    if(CollectionUtils.isEmpty(orderByList)){
                        select.addOrderBy(endInstantTimeStampVar, Order.DESCENDING);
                    }else {
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


    public MoveModel update(MoveModel model) throws Exception {


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

}

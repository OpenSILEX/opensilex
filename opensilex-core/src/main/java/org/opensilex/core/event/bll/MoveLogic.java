/*
 * *****************************************************************************
 *                         MoveLogic.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2024.
 * Last Modification: 17/06/2024 16:02
 * Contact: maximilian.hart@inrae.fr
 * *****************************************************************************
 *
 */

package org.opensilex.core.event.bll;

import com.apicatalog.jsonld.StringUtils;
import com.mongodb.client.ClientSession;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.geojson.Geometry;
import org.apache.commons.collections4.CollectionUtils;
import org.bson.conversions.Bson;
import org.opensilex.core.event.dal.move.*;
import org.opensilex.nosql.distributed.SparqlMongoTransaction;
import org.opensilex.nosql.exceptions.NoSQLInvalidURIException;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.nosql.mongodb.dao.MongoSearchQuery;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.server.exceptions.BadRequestException;
import org.opensilex.server.exceptions.NotFoundURIException;
import org.opensilex.sparql.deserializer.SPARQLDeserializerNotFoundException;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.service.SPARQLResult;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;
import org.opensilex.utils.ThrowingFunction;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import static com.mongodb.client.model.Projections.excludeId;

/**
 *
 *
 * This class contains all logic for Move Events.
 * There is a ClientSession attribute, if it is null then we know we need to handle transactions
 * This is a temporary solution for the embedded transactions problem
 */
public class MoveLogic extends EventLogic<MoveModel, MoveSearchFilter> {

    private final MoveEventNoSqlDao noSqlDao;
    //If client session is null then we know we need to handle transactions
    private final ClientSession clientSession;

    public MoveLogic(SPARQLService sparql, MongoDBService mongodb, AccountModel currentUser, ClientSession clientSession) throws SPARQLException, SPARQLDeserializerNotFoundException {
        super(sparql, mongodb, currentUser, new MoveEventDAO(sparql, mongodb));
        this.noSqlDao = new MoveEventNoSqlDao(mongodb.getServiceV2());
        this.clientSession = clientSession;
    }

    public MoveLogic(SPARQLService sparql, MongoDBService mongodb, AccountModel currentUser) throws SPARQLException, SPARQLDeserializerNotFoundException {
        super(sparql, mongodb, currentUser, new MoveEventDAO(sparql, mongodb));
        this.noSqlDao = new MoveEventNoSqlDao(mongodb.getServiceV2());
        this.clientSession = null;
    }

    //#region PUBLIC METHODS

    @Override
    public void updateModel(MoveModel model) throws Exception{
        check(Collections.singletonList(model), false);

        wrapWithTransaction(session -> updateMoveNoTransaction(model, session));
    }

    @Override
    public void delete(URI uri) throws Exception{

        wrapWithTransaction(session ->{
            deleteNoTransaction(uri, session);
            return 0;
        });

    }

    @Override
    public MoveModel get(URI uri) throws Exception {
        MoveModel model = dao.get(uri, currentUser);
        if (model == null) {
            throw new NotFoundURIException(uri);
        }
        MoveNosqlModel noSqlModel = noSqlDao.get(clientSession, uri);
        if (noSqlModel != null) {
            noSqlModel.setUri(uri);
            model.setNoSqlModel(noSqlModel);
        }
        return model;
    }

    public List<MoveModel> getList(List<URI> uriList) throws Exception {
        var modelList = dao.getList(uriList, currentUser);
        var noSqlModelMap = noSqlDao.getMoveEventNoSqlModelMap(uriList);
        for (var model : modelList) {
            var noSqlModel = noSqlModelMap.get(SPARQLDeserializers.formatURI(model.getUri()));
            if (noSqlModel != null) {
                model.setNoSqlModel(noSqlModel);
            }
        }
        return modelList;
    }

    @Override
    public MoveModel create(MoveModel model) throws Exception {
        return wrapWithTransaction(session -> createNoTransaction(model, session));

    }

    @Override
    public List<MoveModel> create(List<MoveModel> models, boolean validationOnly) throws Exception {
        models.forEach(moveModel -> moveModel.setPublisher(currentUser.getUri()));
        for (var move : models) {
            if (move.getFrom() != null && move.getTo() == null) {
                throw new BadRequestException("Cannot declare a move with a 'From' value but without a 'To' value.");
            }
        }

        check(models, true);

        if(validationOnly){
            return  models;
        }

        List<MoveNosqlModel> noSqlModels = new ArrayList<>();

        // build nosql models
        for (MoveModel model : models) {

            URI uri = model.getUri();
            if (uri == null) {
                uri = model.generateURI(dao.getGraphAsNode().toString(), model, 0);
                model.setUri(uri);
            }

            // set noSql model uri and update noSql model list
            MoveNosqlModel noSqlModel = model.getNoSqlModel();
            if (noSqlModel != null) {
                String shortEventUri = URIDeserializer.getShortURI(model.getUri().toString());
                noSqlModel.setUri(new URI(shortEventUri));
                noSqlModels.add(noSqlModel);
            }

        }

        return wrapWithTransaction(session -> createMultipleNoTransaction(models, noSqlModels, session));
    }

    public MoveNosqlModel getMoveEventNoSqlModel(URI uri) throws NoSuchElementException, NoSQLInvalidURIException {

        Objects.requireNonNull(uri);

        Bson projection = Projections.fields(excludeId());

        MoveNosqlModel model = noSqlDao.get(clientSession, uri, projection);

        model.setUri(uri);
        return model;
    }

    public List<MoveNosqlModel> getIntersectPosition(List<MoveNosqlModel> moveEventNoSqlModelList, Geometry geometry){

        MoveNoSqlSearchFilter filter = new MoveNoSqlSearchFilter();
        filter.setIncludedUris(moveEventNoSqlModelList.stream().map(MoveNosqlModel::getUri).collect(Collectors.toList()));
        filter.setGeometry(geometry);

        return noSqlDao.searchAsStreamWithPagination(filter).getSource().collect(Collectors.toList());
    }

    /**
     * @param target the object on which we get the position
     * @param start  the time at which we search the position
     * @return the position of the given object during a time interval with a descending sort on the move end.
     * @apiNote The method run in two times :
     * <ul>
     * <li> Search corresponding move URI and location from the SPARQL repository </li>
     * <li> Then for each move URI, get the corresponding {@link MoveNosqlModel} from the mongodb collection.
     * <ul>
     * <li> This last operation is done with a single query with a IN filter on {@link MoveNosqlModel#ID_FIELD} field. </li>
     * <li> A {@link Map} between move URI and {@link PositionModel} is maintained in order to associated data from the two databases</li>
     * </ul>
     * </li>
     * </ul>
     * @see MoveEventDAO#search(MoveSearchFilter)
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
        MoveSearchFilter searchFilter = new MoveSearchFilter();
        if(!StringUtils.isBlank(target.toString())){
            searchFilter.setTargets(Collections.singletonList(target));
        }
        searchFilter.setDescriptionPattern(descriptionPattern).setStart(start).setEnd(end);
        searchFilter.setOrderByList(orderByList);
        searchFilter.setPage(page);
        searchFilter.setPageSize(pageSize);
        ListWithPagination<MoveModel> locationHistory = dao.search(searchFilter);

        // Index of position by uri, sorted by event end time
        LinkedHashMap<URI, PositionModel> positionsByUri = new LinkedHashMap<>();

        // for each location, create a position model initialized with the location and update position index
        locationHistory.forEach(move -> {
            positionsByUri.put(move.getUri(), null);
        });


        if (start != null) {
            MoveModel lastMove = getLastMoveAfter(target, start);
            if (lastMove != null) {
                positionsByUri.put(lastMove.getUri(), null);
            }
        }

        if (!positionsByUri.isEmpty()) {

            MoveNoSqlSearchFilter noSqlSearchFilter = new MoveNoSqlSearchFilter();
            noSqlSearchFilter.setIncludedUris(positionsByUri.keySet());
            noSqlSearchFilter.setPage(page);
            noSqlSearchFilter.setPageSize(pageSize);
            Bson concernedItemPositionProjection = getConcernedItemArrayItemProjection(target);

            MongoSearchQuery<MoveNosqlModel, MoveNoSqlSearchFilter, MoveNosqlModel> mongoSearchQuery = new MongoSearchQuery<>();
            mongoSearchQuery.setFilter(noSqlSearchFilter);
            mongoSearchQuery.setProjection(Projections.fields(concernedItemPositionProjection));
            mongoSearchQuery.setConvertFunction(Function.identity());


            noSqlDao.searchAsStreamWithPagination(mongoSearchQuery).getSource().forEach(moveNoSqlModel -> {
                if (!moveNoSqlModel.getTargetPositions().isEmpty()) {
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
            var noSqlModel = new MoveNosqlModel();
            noSqlModel.setTargetPositions(Collections.singletonList(targetPosition));
            move.setNoSqlModel(noSqlModel);
        });
        return locationHistory;
    }

    /**
     * @param concernedItem the object on which we get the last move event
     * @return the last move event of the given object, null if no move event was found.
     */
    public MoveModel getLastMoveEvent(URI concernedItem) throws Exception {
        return this.getLastMoveAfter(concernedItem, null);
    }

    public Map<URI, URI> getLastLocations(Stream<URI> targets, int size) throws Exception {
        return ((MoveEventDAO) dao).getLastLocations(targets, size);
    }

    /**
     * @param target the URI of the object concerned by the {@link MoveModel}
     * @return a {@link SPARQLResult} which the following bindings :
     */
    public MoveModel getLastMoveAfter(URI target, OffsetDateTime dateTime) throws Exception {

        MoveSearchFilter searchFilter = new MoveSearchFilter().setAfterEnd(dateTime);
        searchFilter.setPage(0);
        if(!StringUtils.isBlank(target.toString())){
            searchFilter.setTargets(Collections.singletonList(target));
        }
        searchFilter.setPageSize(1);
        ListWithPagination<MoveModel> moves = dao.search(searchFilter);

        if (CollectionUtils.isEmpty(moves.getList())) {
            return null;
        }

        return moves.getList().get(0);
    }

    /**
     * @param objectUri the object on which we get the position
     * @return the position of the given object at a given time, null if no move event was found.
     * @apiNote if time is null, then the last known position will be returned
     */
    public PositionModel getPosition(URI objectUri, URI moveURI) throws Exception {

        Bson projection = Projections.fields(
                excludeId(), // don't fetch position _id field
                getConcernedItemArrayItemProjection(objectUri) //  don't fetch concernedItem and position of other item
        );

        MoveNosqlModel moveNoSqlModel = noSqlDao.get(clientSession, moveURI, projection);

        if (moveNoSqlModel==null || moveNoSqlModel.getTargetPositions().isEmpty()) {
            return null;
        }

        return moveNoSqlModel.getTargetPositions().get(0).getPosition();
    }

    //#endregion

    //#region PRIVATE METHODS

    /**
     * Runs function either in a transaction or nay
     *
     * @param function to be run
     * @return the result of function
     * @param <R> return type of function
     * @param <E> Some Exception type
     * @throws Exception
     */
    private <R, E extends Exception> R wrapWithTransaction(ThrowingFunction<ClientSession, R, E> function) throws Exception {
        if(this.clientSession == null){
            return new SparqlMongoTransaction(sparql, mongodb.getServiceV2()).execute(function);
        }
        return function.apply(clientSession);
    }

    /**
     * @param concernedItemUri the URI of the item concerned by a move event.
     * @return a {@link Bson} with {@link Filters#eq(Object)} expression on itemPositions.concernedItem property and the given URI
     * @see MoveNosqlModel#getTargetPositions()
     * @see TargetPositionModel#getPosition()
     */
    private Bson getConcernedItemArrayItemProjection(URI concernedItemUri) {
        return Filters.elemMatch(MoveEventNoSqlDao.POSITION_ARRAY_FIELD,
                Filters.eq(MoveEventNoSqlDao.TARGET_FIELD, concernedItemUri)
        );
    }

    private List<MoveModel> createMultipleNoTransaction(List<MoveModel> models, List<MoveNosqlModel> noSqlModels, ClientSession clientSech) throws Exception {
        //insert into sparql graph and nosql
        dao.create(models);
        if (!noSqlModels.isEmpty()) {
            noSqlDao.create(clientSech, noSqlModels);
        }
        return models;
    }

    private MoveModel createNoTransaction(MoveModel model, ClientSession session) throws Exception {
        //Validate and set publisher
        MoveModel realModel = super.create(model);
        // insert move event in mongodb
        MoveNosqlModel noSqlModel = realModel.getNoSqlModel();
        if (noSqlModel != null) {
            String shortEventUri = URIDeserializer.getShortURI(realModel.getUri().toString());
            noSqlModel.setUri(new URI(shortEventUri));
            noSqlDao.create(session, noSqlModel);
        }
        return realModel;
    }

    private void deleteNoTransaction(URI uri, ClientSession session) throws Exception{

        dao.delete(uri);
        boolean moveExistInMongo = noSqlDao.exists(session, uri);

        if (moveExistInMongo) {
            noSqlDao.delete(session, uri);
        }
    }

    private MoveModel updateMoveNoTransaction(MoveModel model, ClientSession session) throws Exception {
        dao.update(model);
        URI modelUri = model.getUri();
        MoveNosqlModel noSqlModel = model.getNoSqlModel();
        boolean moveExistInMongo = noSqlDao.exists(session, modelUri);

        // the new move event has no data model in mongodb, so we need to delete the old if exists
        if (noSqlModel == null) {
            if (moveExistInMongo) {
                noSqlDao.delete(session, modelUri);
            }
        } else {
            noSqlModel.setUri(model.getUri());
            noSqlDao.upsert(session, noSqlModel);
        }
        return model;
    }
    //#endregion
}

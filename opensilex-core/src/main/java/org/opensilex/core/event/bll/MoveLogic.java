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
import com.mongodb.client.model.geojson.Geometry;
import org.apache.commons.collections4.CollectionUtils;
import org.opensilex.core.event.dal.move.*;
import org.opensilex.core.location.api.LocationObservationDTO;
import org.opensilex.core.location.bll.LocationObservationCollectionLogic;
import org.opensilex.core.location.bll.LocationObservationLogic;
import org.opensilex.core.location.dal.LocationObservationModel;
import org.opensilex.nosql.distributed.SparqlMongoTransaction;
import org.opensilex.core.utils.ApiUtils;
import org.opensilex.nosql.exceptions.NoSQLInvalidURIException;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.server.exceptions.NotFoundURIException;
import org.opensilex.sparql.deserializer.SPARQLDeserializerNotFoundException;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.service.SPARQLResult;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;
import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 *
 * This class contains all logic for Move Events.
 * There is a ClientSession attribute, if it is null then we know we need to handle transactions
 * This is a temporary solution for the embedded transactions problem
 */
public class MoveLogic extends EventLogic<MoveModel, MoveSearchFilter> {
    //If client session is null then we know we need to handle transactions
    private final ClientSession clientSession;

    public MoveLogic(SPARQLService sparql, MongoDBService mongodb, AccountModel currentUser, ClientSession clientSession) throws SPARQLException, SPARQLDeserializerNotFoundException {
        super(sparql, mongodb, currentUser, new MoveEventDAO(sparql, mongodb));
        this.clientSession = clientSession;
    }

    public MoveLogic(SPARQLService sparql, MongoDBService mongodb, AccountModel currentUser) throws SPARQLException, SPARQLDeserializerNotFoundException {
        super(sparql, mongodb, currentUser, new MoveEventDAO(sparql, mongodb));
        this.clientSession = null;
    }

    //#region PUBLIC METHODS
    @Override
    public void updateModel(MoveModel model) throws Exception{
        check(Collections.singletonList(model), false);

        ApiUtils.wrapWithTransaction(session -> updateMoveNoTransaction(model, session), this.clientSession, sparql, mongodb);
    }

    @Override
    public void delete(URI uri) throws Exception{

        ApiUtils.wrapWithTransaction(
                session ->{
                    deleteNoTransaction(uri, session);
                    return 0;
                },
                this.clientSession,
                sparql,
                mongodb
        );

    }

    @Override
    public MoveModel get(URI uri) throws Exception {
        MoveModel model = dao.get(uri, currentUser);
        if (Objects.isNull(model)) {
            throw new NotFoundURIException(uri);
        }

        //A move can exist that does not have any positions, if the location observation does not exist then return our Move instead of throwing an error
        LocationObservationCollectionLogic collectionLogic = new LocationObservationCollectionLogic(sparql);
        URI collectionURI = collectionLogic.getLocationObservationCollectionURI(model.getTargets().get(0));

        if(Objects.nonNull(collectionURI)) {
            LocationObservationLogic observationLogic = new LocationObservationLogic(mongodb.getServiceV2());
            LocationObservationModel locationObservation = observationLogic.getASpecificLocationObservation(
                    collectionURI,
                    model.getEnd().getDateTimeStamp().toInstant(),
                    Objects.nonNull(model.getStart()) ? model.getStart().getDateTimeStamp().toInstant() : null);

            model.setLocationObservation(locationObservation);
        }

        return model;
    }

    public List<MoveModel> getList(List<URI> uriList) throws Exception {
        var modelList = dao.getList(uriList, currentUser);

        //for each move, get the target collection URI and use move dates to get the specific location
        LocationObservationCollectionLogic collectionLogic = new LocationObservationCollectionLogic(sparql);
        LocationObservationLogic observationLogic = new LocationObservationLogic(mongodb.getServiceV2());

        Map<URI, URI> targetCollectionMap = collectionLogic.getLocationObservationCollectionList(modelList.stream().map(model -> model.getTargets().get(0)).collect(Collectors.toList()));

        for (var model : modelList) {
            LocationObservationModel location =  observationLogic.getASpecificLocationObservation(
                    targetCollectionMap.get(URI.create(SPARQLDeserializers.getExpandedURI(model.getTargets().get(0)))),
                    model.getEnd().getDateTimeStamp().toInstant(),
                    Objects.nonNull(model.getStart()) ? model.getStart().getDateTimeStamp().toInstant() : null);
            model.setLocationObservation(location);
        }
        return modelList;
    }

    @Override
    public MoveModel create(MoveModel model) throws Exception {
        return ApiUtils.wrapWithTransaction(
                session ->createNoTransaction(model, session),
                this.clientSession,
                sparql,
                mongodb
        );
    }

    @Override
    public List<MoveModel> create(List<MoveModel> models, boolean validationOnly) throws Exception {
        List<LocationObservationModel> locationObservationList = new ArrayList<>();
        LocationObservationLogic observationLogic = new LocationObservationLogic(mongodb.getServiceV2());

        models.forEach(moveModel -> moveModel.setPublisher(currentUser.getUri()));
        check(models, true);

        if(validationOnly){
            return  models;
        }

        for (MoveModel model : models) {
            //Set event move URI
            URI uri = model.getUri();
            if (Objects.isNull(uri)) {
                uri = model.generateURI(dao.getGraphAsNode().toString(), model, 0);
                model.setUri(uri);
            }

            // build location observations
            if (Objects.nonNull(model.getLocationObservation())) {
                model.getTargets().forEach(target -> {
                    LocationObservationModel locationObservation = new LocationObservationModel();
                    LocationObservationCollectionLogic collectionLogic = new LocationObservationCollectionLogic(sparql);

                    try {
                        //Check if a location collection exists for this target
                        URI targetCollection = collectionLogic.getLocationObservationCollectionURI(target);
                        if(Objects.isNull(targetCollection)){
                            targetCollection = collectionLogic.createLocationObservationCollection(target);
                        }

                        locationObservation.setMoveUri(model.getUri());
                        locationObservation.setObservationCollection(targetCollection);
                        locationObservation.setFeatureOfInterest(target);
                        locationObservation.setStartDate(Objects.nonNull(model.getStart()) ? model.getStart().getDateTimeStamp().toInstant() : null);
                        locationObservation.setEndDate(model.getEnd().getDateTimeStamp().toInstant());
                        locationObservation.setLocation(model.getLocationObservation().getLocation());

                        boolean hasGeometry = observationLogic.checkHasGeometry(
                                model.getLocationObservation(),
                                Objects.nonNull(model.getStart()) ? model.getStart().getDateTimeStamp().toInstant() : null,
                                model.getEnd().getDateTimeStamp().toInstant());

                        locationObservation.setHasGeometry(hasGeometry);

                        locationObservationList.add(locationObservation);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }

        return wrapWithTransaction(session -> createMultipleNoTransaction(models, locationObservationList, session));
    }

    /**
     *
     * @param targetUris object list to get last location
     * @param endDate date before location has to be
     * @param intersection the extent where location has to be
     * @return the last location of a target
     * @throws NoSuchElementException
     * @throws SPARQLException
     */
    public Map<URI, LocationObservationModel> getTargetWithPosition(
            List<URI> targetUris,
            Instant endDate,
            Geometry intersection
    ) throws NoSuchElementException, SPARQLException {

        LocationObservationCollectionLogic collectionLogic = new LocationObservationCollectionLogic(sparql);
        Map<URI,URI> targetCollectionMap = collectionLogic.getLocationObservationCollectionList(targetUris);
        LocationObservationLogic locationObservationLogic = new LocationObservationLogic(mongodb.getServiceV2());

        return locationObservationLogic.generateModelObservationCollectionMap(
                targetCollectionMap,
                Objects.nonNull(endDate) ? endDate : Instant.now(),
                true,
                intersection
        );
    }

    /**
     * @param target the object on which we get the position
     * @param start  the time at which we search the position
     * @return the position of the given object during a time interval with a descending sort on the move end.
     * @apiNote The method run in two times :
     * <ul>
     * <li> Search corresponding move URI from the SPARQL repository </li>
     * <li> Then, get the target location history - get corresponding {@link LocationObservationModel} from the mongodb collection.
     * <ul>
     * <li> This last operation is to set the corresponding location in each move.
     * </ul>
     * </li>
     * </ul>
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
        ListWithPagination<MoveModel> moveHistory = dao.search(searchFilter);

        //For each move event get location
        LocationObservationCollectionLogic collectionLogic = new LocationObservationCollectionLogic(sparql);
        URI collectionURI = collectionLogic.getLocationObservationCollectionURI(target);
        List<LocationObservationModel> locationHistory;

        if (Objects.nonNull(collectionURI)) {
            LocationObservationLogic locationObservationLogic = new LocationObservationLogic(mongodb.getServiceV2());
            locationHistory = locationObservationLogic.getLocationsHistory(
                    collectionURI,
                    start != null ? start.toInstant() : null,
                    end != null ? end.toInstant() : null,
                    orderByList,
                    page,
                    pageSize
            ).getList();
        } else {
            locationHistory = new ArrayList<>();
        }

        moveHistory.forEach(move -> locationHistory.forEach(loc ->{
            if(SPARQLDeserializers.compareURIs(loc.getMoveUri(), move.getUri())){
                move.setLocationObservation(loc);
            }
        }));

        return moveHistory;
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
     * @param move move
     * @return the position of the given object at a given time, null if no move event was found.
     * @apiNote if time is null, then the last known position will be returned
     */
    public LocationObservationModel getPosition(MoveModel move) throws Exception {
        LocationObservationCollectionLogic collectionLogic = new LocationObservationCollectionLogic(sparql);
        URI collectionURI = collectionLogic.getLocationObservationCollectionURI(move.getTargets().get(0));

        //Get last location
        LocationObservationLogic locationObservationLogic = new LocationObservationLogic(mongodb.getServiceV2());
        return locationObservationLogic.getASpecificLocationObservation(
                collectionURI,
                move.getEnd().getDateTimeStamp().toInstant(),
                move.getStart() != null ? move.getStart().getDateTimeStamp().toInstant() : null );
    }

    public MoveModel createNoTransaction(MoveModel model, ClientSession session) throws Exception {
        //Validate and set publisher
        MoveModel realModel = super.create(model);
        // insert move event as location in mongodb
        LocationObservationModel observation = realModel.getLocationObservation();

        if (Objects.nonNull(observation)) {
            LocationObservationCollectionLogic collectionLogic = new LocationObservationCollectionLogic(sparql);
            LocationObservationLogic observationLogic = new LocationObservationLogic(mongodb.getServiceV2());

            realModel.getTargets().forEach(target -> {
                try {
                    URI collectionURI = collectionLogic.getLocationObservationCollectionURI(target);

                    if (Objects.isNull(collectionURI)) {
                        collectionURI = collectionLogic.createLocationObservationCollection(target);
                    }

                    Instant end =realModel.getEnd().getDateTimeStamp().toInstant();
                    Instant start = Objects.nonNull(realModel.getStart()) ? realModel.getStart().getDateTimeStamp().toInstant() : null;

                    observationLogic.validateDates(end, start);

                    boolean hasGeometry = observationLogic.checkHasGeometry(
                            observation,
                            start,
                            end);

                    observationLogic.createLocationObservation(
                            session,
                            collectionURI,
                            target,
                            hasGeometry,
                            start,
                            end,
                            observation.getLocation(),
                            realModel.getUri()
                    );
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }

        return realModel;
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

    private List<MoveModel> createMultipleNoTransaction(List<MoveModel> models, List<LocationObservationModel> locations, ClientSession clientSech) throws Exception {
        //insert into sparql graph and nosql
        dao.create(models);
        if (!locations.isEmpty()) {
            LocationObservationLogic observationLogic = new LocationObservationLogic(mongodb.getServiceV2());
            observationLogic.createLocationObservations(clientSech, locations, sparql, currentUser);
        }
        return models;
    }

    private void deleteNoTransaction(URI uri, ClientSession session) throws Exception{
        MoveModel model= dao.get(uri,currentUser);

        LocationObservationCollectionLogic collectionLogic = new LocationObservationCollectionLogic(sparql);
        URI collectionURI = collectionLogic.getLocationObservationCollectionURI(model.getTargets().get(0));

        LocationObservationLogic observationLogic = new LocationObservationLogic(mongodb.getServiceV2());
        observationLogic.deleteASpecificLocationObservation(
                session,
                collectionURI,
                model.getEnd().getDateTimeStamp().toInstant(),
                Objects.nonNull(model.getStart()) ? model.getStart().getDateTimeStamp().toInstant() : null
                );

        dao.delete(uri);
    }

    private MoveModel updateMoveNoTransaction(MoveModel model, ClientSession session) throws Exception {
        LocationObservationCollectionLogic collectionLogic = new LocationObservationCollectionLogic(sparql);
        URI collectionURI = collectionLogic.getLocationObservationCollectionURI(model.getTargets().get(0));

        LocationObservationLogic observationLogic = new LocationObservationLogic(mongodb.getServiceV2());
        LocationObservationModel observation = observationLogic.getASpecificLocationObservation(collectionURI, model.getLocationObservation().getEndDate(), model.getLocationObservation().getStartDate());

        // the new move event has no data model in mongodb, so we need to delete the old if exists
        if (Objects.isNull(model.getLocationObservation())) {
            if (Objects.nonNull(observation)) {
                observationLogic.deleteASpecificLocationObservation(session, collectionURI,observation.getEndDate(),Objects.nonNull(model.getStart()) ? model.getStart().getDateTimeStamp().toInstant() : null);
                //if the feature of interest doesn't have location observations, delete the collection
                int count = observationLogic.countLocationsForURI(collectionURI);
                if(count == 0){
                    collectionLogic.deleteLocationObservationCollection(collectionURI);
                }
            }
        } else {
            LocationObservationModel newLocation = model.getLocationObservation();

            boolean hasGeometry = observationLogic.checkHasGeometry(
                    newLocation,
                    Objects.nonNull(model.getStart()) ? model.getStart().getDateTimeStamp().toInstant() : null,
                    model.getEnd().getDateTimeStamp().toInstant());

            newLocation.setObservationCollection(collectionURI);
            newLocation.setFeatureOfInterest(model.getTargets().get(0));
            newLocation.setStartDate(Objects.nonNull(model.getStart()) ? model.getStart().getDateTimeStamp().toInstant() : null);
            newLocation.setEndDate(model.getEnd().getDateTimeStamp().toInstant());
            newLocation.setHasGeometry(hasGeometry);

            observationLogic.updateASpecificLocationObservation(session,observation, newLocation);
        }

        dao.update(model);
        return model;
    }
    //#endregion
}

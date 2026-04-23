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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.mongodb.client.ClientSession;
import org.apache.commons.collections4.CollectionUtils;
import org.eclipse.rdf4j.query.algebra.Move;
import org.locationtech.jts.io.ParseException;
import org.opensilex.core.event.api.move.MoveCreationDTO;
import org.opensilex.core.event.dal.move.*;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.geospatial.dal.GeospatialDAO;
import org.opensilex.core.location.api.LocationObservationDTO;
import org.opensilex.core.location.bll.LocationObservationCollectionLogic;
import org.opensilex.core.location.bll.LocationObservationLogic;
import org.opensilex.core.location.dal.LocationModel;
import org.opensilex.core.location.dal.LocationObservationCollectionModel;
import org.opensilex.core.location.dal.LocationObservationModel;
import org.opensilex.core.location.dal.LocationObservationSearchFilter;
import org.opensilex.core.ontology.Oeev;
import org.opensilex.core.position.api.TargetPositionCreationDTO;
import org.opensilex.core.scientificObject.bll.ScientificObjectCsvImporterLogic;
import org.opensilex.core.utils.ApiUtils;
import org.opensilex.core.utils.StringUriMap;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.server.exceptions.BadRequestException;
import org.opensilex.server.exceptions.InvalidValueException;
import org.opensilex.server.exceptions.NotFoundURIException;
import org.opensilex.sparql.deserializer.SPARQLDeserializerNotFoundException;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLResult;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.*;
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

    /**
     * If the moves LocationObservation is null then we create an empty one with an empty Location inside it.
     *
     * @param model the Move for whom we want to get or create its LocationObservation.
     * @param experimentUri If the move is being performed in the scope of an experiment then we can set the LocationObservation's
     *                      experiment field upon creation. Null is considered as a global scope.
     * @return the corresponding created or gotten LocationModel
     */
    public static LocationModel getOrCreateMovesLocationObservation(MoveModel model, URI experimentUri){
        LocationModel locationModel;
        if(model.getLocationObservation() == null){
            locationModel = new LocationModel();
            LocationObservationModel locationObservationModel = new LocationObservationModel();
            locationObservationModel.setLocation(locationModel);
            locationObservationModel.setExperimentUri(experimentUri);
            model.setLocationObservation(locationObservationModel);
        }else{
            locationModel = model.getLocationObservation().getLocation();
        }
        return locationModel;
    }

    /**
     *
     * @param targets the target URIs for whom we want to fetch all moves
     * @param experimentUri can be null, if it is not null then we exclude moves whose locations were not recorded in this Experiment
     * @return A StringUriMap with OS URIs as keys, and it's corresponding oldest move as values.
     */
    public StringUriMap<MoveModel> getInitialMovesWithLocationPerTarget(List<URI> targets, URI experimentUri) throws Exception{
        StringUriMap<List<MoveModel>> allMovesPerTarget = getMovesWithLocationPerTarget(targets, experimentUri);
        StringUriMap<MoveModel> result = new StringUriMap<>();
        allMovesPerTarget.forEach((targetUriAsString,  moveModels) -> {
            MoveModel mostAncientMove = moveModels.get(0);//Normally can't fail as previous operation ensures there will be at least 1 move
            //Just need to look at endDate as that is the obligatory one
            for(int currentMovesIndex = 1; currentMovesIndex < moveModels.size(); currentMovesIndex++){
                MoveModel nextMove = moveModels.get(currentMovesIndex);
                if(nextMove.getEnd().getDateTimeStamp().isBefore(mostAncientMove.getEnd().getDateTimeStamp())){
                    mostAncientMove = nextMove;
                }
            }
            result.put(URI.create(targetUriAsString), mostAncientMove);
        });
        return result;
    }

    /**
     *
     * @param targets the target URIs for whom we want to fetch all moves
     * @param experimentUri can be null, if it is not null then we exclude moves whose locations were not recorded in this Experiment
     * @return A StringUriMap with OS URIs as keys, and it's corresponding list of moves as values.
     */
    public StringUriMap<List<MoveModel>> getMovesWithLocationPerTarget(List<URI> targets, URI experimentUri) throws Exception {
        MoveSearchFilter moveSearchFilter = new MoveSearchFilter();
        moveSearchFilter
                .setType(URI.create(Oeev.Move.getURI()))
                .setTargets(targets);
        moveSearchFilter.setPageSize(0);
        List<MoveModel> allExistingMoves = searchMovesWithLocationObservation(moveSearchFilter).getList();

        //Remove any moves that don't have a location or have a location that doesn't belong in this experiment
        if(experimentUri != null){
            allExistingMoves = allExistingMoves.stream().filter(move ->
                    move.getLocationObservation() != null && move.getLocationObservation().getExperimentUri() != null && SPARQLDeserializers.compareURIs(move.getLocationObservation().getExperimentUri(), experimentUri)
            ).toList();
        }

        //Map to check every Location, at the end we'll get a map of OS ShortUriString -> List<Moves>, if a target has
        //Multiple Moves then we know we need to handle separately by checking dates.
        StringUriMap<List<MoveModel>> visitedMovesPerTargets = new StringUriMap<>();
        allExistingMoves
                .stream()
                .forEach(existingMove ->{
                    //We know move targets can't be null as we did a search filtering by target
                    List<MoveModel> movesForTarget = visitedMovesPerTargets.computeIfAbsent(existingMove.getTargets().get(0), (key)-> new ArrayList<>());
                    movesForTarget.add(existingMove);
                });
        return visitedMovesPerTargets;
    }

    /**
     * Separate function as the main api usage of search is converted into a EventDTO,
     * so no need for Location information in the main case
     * @param filter the search filter to search moves by.
     * @return A list of MoveModels with their LocationObservation's filled
     */
    public ListWithPagination<MoveModel> searchMovesWithLocationObservation(MoveSearchFilter filter) throws Exception {
        //Get moves
        ListWithPagination<MoveModel> moves = dao.search(filter);
        if(CollectionUtils.isEmpty(moves.getList())){
            return moves;
        }
        //Get locations
        List<LocationObservationModel> correspondingLocObs =
                getMovesCorrespondingLocationObservationModels(
                        moves
                                .getList()
                                .stream()
                                .map(MoveModel::getUri).toList(),
                        new LocationObservationLogic(mongodb.getServiceV2(), sparql)
                );
        //Associate locations to moves
        StringUriMap<LocationObservationModel> locationPerMoveUri = new StringUriMap<>();
        for(LocationObservationModel locObsModel : correspondingLocObs){
            locationPerMoveUri.put(locObsModel.getMoveUri(), locObsModel);
        }
        for(MoveModel move : moves.getList()){
            move.setLocationObservation(locationPerMoveUri.get(move.getUri()));
        }
        return moves;
    }

    @Override
    public void updateModel(MoveModel model) throws Exception{
        check(Collections.singletonList(model), false);

        ApiUtils.wrapWithTransaction(session -> updateMoveNoTransaction(model, session), this.clientSession, sparql, mongodb);
    }

    @Override
    public void updateModels(List<MoveModel> models) throws Exception{
        if(CollectionUtils.isEmpty(models)){
            return;
        }
        check(models, false);
        ApiUtils.wrapWithTransaction(session -> updateExistingMovesNoTransaction(models, session), this.clientSession, sparql, mongodb);
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
    public void deleteList(List<URI> uris) throws Exception{
        if(CollectionUtils.isEmpty(uris)){
            return;
        }
        ApiUtils.wrapWithTransaction(
                session ->{
                    deleteListNoTransaction(uris, session);
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
            LocationObservationLogic observationLogic = new LocationObservationLogic(mongodb.getServiceV2(), sparql);
            LocationObservationModel locationObservation = observationLogic.getASpecificLocationObservation(
                    collectionURI,
                    model.getEnd().getDateTimeStamp().toInstant(),
                    Objects.nonNull(model.getStart()) ? model.getStart().getDateTimeStamp().toInstant() : null);

            model.setLocationObservation(locationObservation);
        }

        return model;
    }

    /**
     * The get list of Moves function. For their locations a special operation has to be performed as their is no direct mapping,
     * the location fetching operation also involves MongoDB.
     * To link the correct LocationObservation, it needs to belong to LocationObservationCollection
     * whose featureOfInterest, is the target of the move in question. And from the collection, we need to fetch the LocationObservation whose
     * dates match our move's dates.
     *
     * @param uriList uris of the Moves we are getting
     * @return the list of corresponding MoveModels, with their LocationObservation properly filled.
     */
    public List<MoveModel> getList(List<URI> uriList) throws Exception {
        var modelList = dao.getList(uriList, currentUser);

        //for each move, get the target collection URI and use move dates to get the specific location
        LocationObservationCollectionLogic collectionLogic = new LocationObservationCollectionLogic(sparql);
        LocationObservationLogic observationLogic = new LocationObservationLogic(mongodb.getServiceV2(), sparql);

        Map<URI, URI> targetCollectionMap = collectionLogic.getLocationObservationCollectionPerFeatureOfInterest(modelList.stream().map(model -> model.getTargets().get(0)).collect(Collectors.toList()));

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

    /**
     * A specialized faster function to create moves where we know each move has a single target, and it is the first time
     * creating a move on each target. Will be used to create moves in conjunction with Scientific Objects during CSV import.
     * Made this specialized function instead of adding a boolean to the main create function as the case seems quite specific to
     * Moves, no need to pollute EventLogic's create(List) method with an extra boolean.
     *
     * @param models the Moves to create
     * @return the newly inserted Moves with their URI's filled. And their LocationObservations have their newly created
     * LocationObservationCollection URIs. Handles setting of MoveUri in the LocationObservations.
     *
     */
    public List<MoveModel> createFirstSingleUniqueTargetMoves(List<MoveModel> models) throws Exception {
        if(CollectionUtils.isEmpty(models)){
            return models;
        }
        //Set publishers and run basic Events check
        models.forEach(moveModel -> moveModel.setPublisher(currentUser.getUri()));
        check(models, true);

        //Create Location collections if they don't already exist
        LocationObservationCollectionLogic locObsCollectionLogic = new LocationObservationCollectionLogic(sparql);

        List<URI> allTargets = models.stream().map(model -> model.getTargets().get(0)).toList();

        //Fetch existing collections (can happen if a target already had a location in another experiment)
        Map<URI, URI> existingCollectionsPerTarget = locObsCollectionLogic.getLocationObservationCollectionPerFeatureOfInterest(allTargets);
        StringUriMap<URI> locCollectionUriPerTarget = new StringUriMap<>(existingCollectionsPerTarget);

        //Create collections for any target that never had a location
        List<URI> targetsThatDontHaveCollection = locCollectionUriPerTarget.removeAlreadyPresentKeysFromList(allTargets);
        if(!CollectionUtils.isEmpty(targetsThatDontHaveCollection)){
            List<LocationObservationCollectionModel> newlyCreatedCollections = locObsCollectionLogic
                    .createList(targetsThatDontHaveCollection);
            //Add the new Target -> collection entries to map
            for(LocationObservationCollectionModel nextLocObsCollectionModel : newlyCreatedCollections){
                locCollectionUriPerTarget.put(nextLocObsCollectionModel.getFeatureOfInterest(), nextLocObsCollectionModel.getUri());
            }
        }

        //Actually create Moves and Locations
        return createMoveModelsFromTargetToLocationCollectionMap(models, locCollectionUriPerTarget);
    }

    @Override
    public List<MoveModel> create(List<MoveModel> models, boolean validationOnly) throws Exception {

        models.forEach(moveModel -> moveModel.setPublisher(currentUser.getUri()));
        check(models, true);

        if(validationOnly){
            return  models;
        }

        //Fetch already existing collections
        LocationObservationCollectionLogic locObsCollectionLogic = new LocationObservationCollectionLogic(sparql);
        Set<String> uniqueTargets = new HashSet<>();
        for(MoveModel model : models){
            for(URI target : model.getTargets()){
                uniqueTargets.add(SPARQLDeserializers.getShortURI(target));
            }
        }

        Map<URI, URI> collectionUriPerTarget = locObsCollectionLogic
                .getLocationObservationCollectionPerFeatureOfInterest(uniqueTargets.stream().map(URI::create).toList());

        //Create target->LocationCollectionUri mapping for the already existing collections
        StringUriMap<URI> locCollectionUriPerTarget = new StringUriMap<>(collectionUriPerTarget);

        //Filter out targets for whom they already have a location collection
        Set<String> targetsWithExistingCollection = locCollectionUriPerTarget.keySet();
        uniqueTargets.removeAll(targetsWithExistingCollection);

        //Now create collections for these and add to mapping
        if(!CollectionUtils.isEmpty(uniqueTargets)){
            List<LocationObservationCollectionModel> newlyCreatedCollections = locObsCollectionLogic
                    .createList(uniqueTargets.stream().map(URI::create).toList());
            for(LocationObservationCollectionModel nextNewCollection : newlyCreatedCollections){
                locCollectionUriPerTarget.put(nextNewCollection.getFeatureOfInterest(), nextNewCollection.getUri());
            }
        }

        return createMoveModelsFromTargetToLocationCollectionMap(models, locCollectionUriPerTarget);
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
            LocationObservationLogic locationObservationLogic = new LocationObservationLogic(mongodb.getServiceV2(), sparql);
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
        LocationObservationLogic locationObservationLogic = new LocationObservationLogic(mongodb.getServiceV2(), sparql);
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
            LocationObservationLogic observationLogic = new LocationObservationLogic(mongodb.getServiceV2(), sparql);

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

    /**
     * Sets the string value to be the Facility URI corresponding to the moves too field.
     * Handles creation of Location and LocationObservation models if not yet present.
     * This method does NOT interact with database, just effects the model!
     *
     * @param toString a string value that we know is supposed to be uri
     * @param movesLocation the LocationModel that will be associated to Move
     * @return true if value was not empty, false otherwise
     */
    public boolean applyValueOnTooField(String toString, LocationModel movesLocation) throws URISyntaxException, InvalidValueException, SPARQLException {
        if(!org.apache.commons.lang3.StringUtils.isEmpty(toString)) {
            movesLocation.setTo(new URI(toString));
            return true;
        }
        return false;
    }

    /**
     * Sets the string value to be the Facility URI corresponding to the moves from field.
     * Handles creation of Location and LocationObservation models if not yet present.
     * This method does NOT interact with database, just effects the model!
     *
     * @param fromString a string value that we know is supposed to be uri
     * @param movesLocation the LocationModel that will be associated to Move
     * @return true if value was not empty, false otherwise
     */
    public boolean applyValueOnFromField(String fromString, LocationModel movesLocation) throws URISyntaxException {
        if(!org.apache.commons.lang3.StringUtils.isEmpty(fromString)) {
            movesLocation.setFrom(new URI(fromString));
            return true;
        }
        return false;
    }

    /**
     * Tries to convert the string wkt into a Geometry object, so we can set this to be Location's geometry field.
     * Handles creation of Location and LocationObservation models if not yet present.
     * This method does NOT interact with database, just effects the model!
     *
     * @param geometryString expected in wkt format
     * @param movesLocation the LocationModel that will be associated to Move
     * @return true if value was not empty, false otherwise
     */
    public boolean applyValueOnGeometryField(String geometryString, LocationModel movesLocation) throws ParseException, JsonProcessingException{
        if(!org.apache.commons.lang3.StringUtils.isEmpty(geometryString)){
            movesLocation.setGeometry( GeospatialDAO.wktToGeometry(geometryString));
            return true;
        }
        return false;
    }

    /**
     * Sets the string value to be the Location's x value of the passed Move.
     * Handles creation of Location and LocationObservation models if not yet present.
     * This method does NOT interact with database, just effects the model!
     *
     * @param xString a string value representing x
     * @param movesLocation the LocationModel that will be associated to Move
     * @return true if value was not empty, false otherwise
     */
    public boolean applyValueOnXField(String xString, LocationModel movesLocation) {
        if(!org.apache.commons.lang3.StringUtils.isEmpty(xString)) {
            movesLocation.setX(xString);
            return true;
        }
        return false;
    }

    /**
     * Sets the string value to be the Location's y value of the passed Move.
     * Handles creation of Location and LocationObservation models if not yet present.
     * This method does NOT interact with database, just effects the model!
     *
     * @param yString a string value representing y
     * @param movesLocation the LocationModel that will be associated to Move
     * @return true if value was not empty, false otherwise
     */
    public boolean applyValueOnYField(String yString, LocationModel movesLocation) {
        if(!org.apache.commons.lang3.StringUtils.isEmpty(yString)) {
            movesLocation.setY(yString);
            return true;
        }
        return false;
    }

    /**
     * Sets the string value to be the Location's z value of the passed Move.
     * Handles creation of Location and LocationObservation models if not yet present.
     * This method does NOT interact with database, just effects the model!
     *
     * @param zString a string value representing z
     * @param movesLocation the LocationModel that will be associated to Move
     * @return true if value was not empty, false otherwise
     */
    public boolean applyValueOnZField(String zString, LocationModel movesLocation) {
        if(!org.apache.commons.lang3.StringUtils.isEmpty(zString)) {
            movesLocation.setZ(zString);
            return true;
        }
        return false;
    }

    /**
     * Sets the string value to be the Location's textual value of the passed Move.
     * Handles creation of Location and LocationObservation models if not yet present.
     * This method does NOT interact with database, just effects the model!
     *
     * @param textual a string value representing the textual location (example : Brian is in the kitchen)
     * @param movesLocation the LocationModel that will be associated to Move
     * @return true if value was not empty, false otherwise
     */
    public boolean applyValueOnTextualField(String textual, LocationModel movesLocation) {
        if(!org.apache.commons.lang3.StringUtils.isEmpty(textual)) {
            movesLocation.setTextualPosition(textual);
            return true;
        }
        return false;
    }

    /**
     * In order to keep deprecated properties without relying on them, we need to convert old 'from', 'to' and 'targets_positions' to new property location
     * DTOs with a location will be ignored, no changes will be applied on them.
     * DTOs with multiple targets_positions will be split in many MoveCreationDTOs
     */
    public void fillLocationPropertyWhenNeededForRetrocompatibilityPurposes(List<MoveCreationDTO> dtos){
        // move with multiple targets_positions will be splited in new moves that we will append to the dtos list
        List<MoveCreationDTO> dtosToAddFromMultipleTargetsPositions = new ArrayList<>();

        for ( MoveCreationDTO moveDTO : dtos){
            if (moveDTO.getLocation() != null) {
                continue;
            }

            LocationObservationDTO firstLocationDTO = new LocationObservationDTO();
            firstLocationDTO.setFrom(moveDTO.getFrom());
            firstLocationDTO.setTo(moveDTO.getTo());

            boolean isFirstTargetPosition = true;
            for (TargetPositionCreationDTO targetPositionDTO : moveDTO.getTargetsPositions()) {
                LocationObservationDTO currentLocationDTO = new LocationObservationDTO();
                if (Objects.nonNull(moveDTO.getFrom()) || Objects.nonNull(moveDTO.getTo())) {

                    if (moveDTO.getTargetsPositions() != null && !moveDTO.getTargetsPositions().isEmpty()) {
                        currentLocationDTO.setFrom(moveDTO.getFrom());
                        currentLocationDTO.setTo(moveDTO.getTo());
                        currentLocationDTO.setFeatureOfInterest(targetPositionDTO.getTarget());
                        if (Objects.nonNull(targetPositionDTO.getPosition())) {
                            currentLocationDTO.setX(targetPositionDTO.getPosition().getX());
                            currentLocationDTO.setY(targetPositionDTO.getPosition().getY());
                            currentLocationDTO.setZ(targetPositionDTO.getPosition().getZ());
                            currentLocationDTO.setTextualPosition(targetPositionDTO.getPosition().getDescription());
                            currentLocationDTO.setGeojson(targetPositionDTO.getPosition().getPoint());
                        }
                    }


                    if (isFirstTargetPosition){
                        firstLocationDTO = currentLocationDTO;
                        isFirstTargetPosition = false;
                    } else {
                        // if it is not the first target position, create a new Move and add it to the list
                        MoveCreationDTO newMoveDTO = new MoveCreationDTO();
                        newMoveDTO.setTargets(moveDTO.getTargets());
                        newMoveDTO.setIsInstant(moveDTO.getIsInstant());
                        newMoveDTO.setStart(moveDTO.getStart());
                        newMoveDTO.setEnd(moveDTO.getEnd());
                        newMoveDTO.setDescription(moveDTO.getDescription());
                        newMoveDTO.setType(moveDTO.getType());
                        newMoveDTO.setRelations(moveDTO.getRelations());

                        newMoveDTO.setLocation(currentLocationDTO);
                        dtosToAddFromMultipleTargetsPositions.add(newMoveDTO);
                    }
                }
            }

            moveDTO.setLocation(firstLocationDTO); //first location is set on the base moveDTO
        }
    }
    //#endregion

    //#region PRIVATE METHODS
    private List<MoveModel> createMoveModelsFromTargetToLocationCollectionMap(List<MoveModel> models, StringUriMap<URI> locCollectionUriPerTarget) throws Exception {
        List<LocationObservationModel> locationObservationList = new ArrayList<>();
        LocationObservationLogic observationLogic = new LocationObservationLogic(mongodb.getServiceV2(), sparql);
        for (MoveModel model : models) {
            //Set event move URI
            URI uri = model.getUri();
            if (Objects.isNull(uri)) {
                //Generate URI manually before insertion as the LocationObservation needs this information.
                uri = model.generateURI(dao.getGraphAsNode().toString(), model, 0);
                model.setUri(uri);
            }

            // build location observations
            if (Objects.nonNull(model.getLocationObservation())) {
                model.getTargets().forEach(target -> {
                    LocationObservationModel locationObservation = new LocationObservationModel();

                    try {
                        //Get corresponding collection uri from passed mapping
                        URI targetCollection = locCollectionUriPerTarget.get(target);

                        locationObservation.setMoveUri(model.getUri());
                        locationObservation.setObservationCollection(targetCollection);
                        locationObservation.setFeatureOfInterest(target);
                        locationObservation.setStartDate(Objects.nonNull(model.getStart()) ? model.getStart().getDateTimeStamp().toInstant() : null);
                        locationObservation.setEndDate(model.getEnd().getDateTimeStamp().toInstant());
                        locationObservation.setLocation(model.getLocationObservation().getLocation());
                        locationObservation.setExperimentUri(model.getLocationObservation().getExperimentUri());

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

        return ApiUtils.wrapWithTransaction(session -> createMultipleNoTransaction(models, locationObservationList, session), clientSession, sparql, mongodb);
    }

    private void deleteListNoTransaction(List<URI> uris, ClientSession session) throws Exception{
        //Delete associated Location Observations
        LocationObservationLogic locObsLogic = new LocationObservationLogic(mongodb.getServiceV2(), sparql);
        List<LocationObservationModel> allExistingLocObs = getMovesCorrespondingLocationObservationModels(uris, locObsLogic);
        locObsLogic.deleteList(session, allExistingLocObs);
        //Delete actual moves
        dao.deleteMany(uris);
    }

    private List<MoveModel> createMultipleNoTransaction(List<MoveModel> models, List<LocationObservationModel> locations, ClientSession clientSech) throws Exception {
        //insert into sparql graph and nosql
        dao.create(models);
        if (!locations.isEmpty()) {
            LocationObservationLogic observationLogic = new LocationObservationLogic(mongodb.getServiceV2(), sparql);
            observationLogic.createLocationObservations(clientSech, locations, sparql, currentUser);
        }
        return models;
    }

    private void deleteNoTransaction(URI uri, ClientSession session) throws Exception{
        MoveModel model= dao.get(uri,currentUser);

        LocationObservationCollectionLogic collectionLogic = new LocationObservationCollectionLogic(sparql);
        URI collectionURI = collectionLogic.getLocationObservationCollectionURI(model.getTargets().get(0));

        LocationObservationLogic observationLogic = new LocationObservationLogic(mongodb.getServiceV2(), sparql);
        observationLogic.deleteASpecificLocationObservation(
                session,
                collectionURI,
                model.getEnd().getDateTimeStamp().toInstant(),
                Objects.nonNull(model.getStart()) ? model.getStart().getDateTimeStamp().toInstant() : null
                );

        dao.delete(uri);
    }

    /**
     * We know that every move has 1 Location observation, this function fetches each one for each passed move.
     *
     * @param modelUris the move models uris we need Location observations for
     * @param locObsLogic location observation logic object
     * @return
     */
    private List<LocationObservationModel> getMovesCorrespondingLocationObservationModels(
            List<URI> modelUris,
            LocationObservationLogic locObsLogic
    ){
        if(CollectionUtils.isEmpty(modelUris)){
            return Collections.emptyList();
        }
        LocationObservationSearchFilter locationObservationSearchFilter = new LocationObservationSearchFilter();
        locationObservationSearchFilter.setMoveUris(modelUris);
        locationObservationSearchFilter.setPageSize(ScientificObjectCsvImporterLogic.LOCATION_FETCHING_PAGE_SIZE);
        List<LocationObservationModel> allExistingLocObs = new ArrayList<>();
        int currentPage = 0;

        //Do it page by page, unless there's something i'm missing you cant just do a standard non paginated search with the MongoReadWriteDao
        boolean doContinue = true;
        while(doContinue){
            locationObservationSearchFilter.setPage(currentPage);
            List<LocationObservationModel> nextExistingLocObs = locObsLogic
                    .searchLocationObservations(locationObservationSearchFilter).getList();
            allExistingLocObs.addAll(nextExistingLocObs);
            doContinue = nextExistingLocObs.size() == ScientificObjectCsvImporterLogic.LOCATION_FETCHING_PAGE_SIZE;
            currentPage++;
        }
        return allExistingLocObs;
    }

    /**
     *
     * @param models new versions of models we are updating, these models must be existing, no handling of deletes or creates in this method
     * @param session Client session
     * @return the list of newly updated moves
     * @throws Exception if some sparql error happens
     */
    private List<MoveModel> updateExistingMovesNoTransaction(List<MoveModel> models, ClientSession session) throws Exception{
        LocationObservationLogic locObsLogic = new LocationObservationLogic(mongodb.getServiceV2(), sparql);
        List<LocationObservationModel> allExistingLocObs = getMovesCorrespondingLocationObservationModels(models.stream().map(MoveModel::getUri).toList(), locObsLogic);

        StringUriMap<LocationObservationModel> locObsByMoveUri = new StringUriMap<>();
        for(LocationObservationModel locObsModel : allExistingLocObs){
            locObsByMoveUri.put(locObsModel.getMoveUri(), locObsModel);
        }

        List<LocationObservationModel> finalLocationsToUpdate = new ArrayList<>();
        for(MoveModel model : models){
            LocationObservationModel existingLocObsModel = locObsByMoveUri.get(model.getUri());
            applyLocObsFieldsToSetFromMove(model, existingLocObsModel.getObservationCollection(), locObsLogic);
            model.getLocationObservation().setUri(existingLocObsModel.getUri());
            finalLocationsToUpdate.add(model.getLocationObservation());
        }

        //update location observations in mongo
        locObsLogic.updateList(session, finalLocationsToUpdate);

        //update Moves
        dao.updateModels(models);
        return models;
    }

    private void applyLocObsFieldsToSetFromMove(MoveModel model, URI collectionURI, LocationObservationLogic observationLogic){
        LocationObservationModel newLocation = model.getLocationObservation();

        boolean hasGeometry = observationLogic.checkHasGeometry(
                newLocation,
                Objects.nonNull(model.getStart()) ? model.getStart().getDateTimeStamp().toInstant() : null,
                model.getEnd().getDateTimeStamp().toInstant());

        newLocation.setMoveUri(model.getUri());
        newLocation.setObservationCollection(collectionURI);
        newLocation.setFeatureOfInterest(model.getTargets().get(0));
        newLocation.setStartDate(Objects.nonNull(model.getStart()) ? model.getStart().getDateTimeStamp().toInstant() : null);
        newLocation.setEndDate(model.getEnd().getDateTimeStamp().toInstant());
        newLocation.setHasGeometry(hasGeometry);
    }

    private MoveModel updateMoveNoTransaction(MoveModel model, ClientSession session) throws Exception {
        LocationObservationCollectionLogic collectionLogic = new LocationObservationCollectionLogic(sparql);
        URI collectionURI = collectionLogic.getLocationObservationCollectionURI(model.getTargets().get(0));

        LocationObservationLogic observationLogic = new LocationObservationLogic(mongodb.getServiceV2(), sparql);
        LocationObservationModel observation = observationLogic.getASpecificLocationObservation(collectionURI, model.getLocationObservation().getEndDate(), model.getLocationObservation().getStartDate());

        // the new move event has no data model in mongodb, so we need to delete the old if exists
        if (Objects.isNull(model.getLocationObservation())) {
            if (Objects.nonNull(observation)) {
                observationLogic.deleteASpecificLocationObservation(session, collectionURI,observation.getEndDate(),Objects.nonNull(model.getStart()) ? model.getStart().getDateTimeStamp().toInstant() : null);
                //if the feature of interest doesn't have location observations, delete the collection
                int count = observationLogic.countLocationsForCollectionURI(collectionURI);
                if(count == 0){
                    collectionLogic.deleteLocationObservationCollection(collectionURI);
                }
            }
        } else {
            applyLocObsFieldsToSetFromMove(model, collectionURI, observationLogic);
            observationLogic.updateASpecificLocationObservation(session,observation, model.getLocationObservation());
        }

        dao.update(model);
        return model;
    }
    //#endregion
}

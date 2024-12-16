/*
 * *****************************************************************************
 *                         LocationObservationLogic.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2024.
 * Last Modification: 26/07/2024 13:35
 * Contact: alexia.chiavarino@inrae.fr
 * *****************************************************************************
 *
 */

package org.opensilex.core.location.bll;

import com.mongodb.client.ClientSession;
import com.mongodb.client.model.geojson.Geometry;
import org.opensilex.core.location.dal.LocationModel;
import org.opensilex.core.location.dal.LocationObservationDAO;
import org.opensilex.core.location.dal.LocationObservationModel;
import org.opensilex.core.location.dal.LocationObservationCollectionModel;
import org.opensilex.core.location.dal.LocationObservationSearchFilter;
import org.opensilex.nosql.exceptions.NoSQLAlreadyExistingUriException;
import org.opensilex.nosql.exceptions.NoSQLInvalidURIException;
import org.opensilex.nosql.mongodb.service.v2.MongoDBServiceV2;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.server.exceptions.NotFoundURIException;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;

import javax.ws.rs.NotAllowedException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LocationObservationLogic {

    private final LocationObservationDAO locationObservationDAO;

    //#region constructor
    public LocationObservationLogic(MongoDBServiceV2 nosql) {
        this.locationObservationDAO = new LocationObservationDAO(nosql);
    }
    //#endregion

    //#region public
    public void createLocationObservation(ClientSession session, URI locationObservationCollectionURI, URI featureOfInterest, boolean hasGeometry, Instant startDate, Instant endDate, LocationModel locationModel,URI moveURI) throws NoSQLAlreadyExistingUriException, URISyntaxException {
        validateDates(endDate, startDate);

        LocationObservationModel locationObservationModel = new LocationObservationModel();

        locationObservationModel.setLocation(locationModel);
        locationObservationModel.setEndDate(endDate);
        if (Objects.nonNull(startDate)) {
                locationObservationModel.setStartDate(startDate);
        }

        locationObservationModel.setObservationCollection(locationObservationCollectionURI);
        locationObservationModel.setFeatureOfInterest(featureOfInterest);
        locationObservationModel.setHasGeometry(hasGeometry);
        if(Objects.nonNull(moveURI)) {
            locationObservationModel.setUri(moveURI);
        }

        locationObservationDAO.create(session, locationObservationModel);
    }

    public void createLocationObservations(ClientSession session, List<LocationObservationModel> observations) throws Exception {
        validateCollectionsConsistency(observations);

        locationObservationDAO.create(session, observations);
    }

    public void createLocationObservations(ClientSession session, URI locationObservationCollectionURI, URI featureOfInterest, List<LocationObservationModel> models, boolean hasGeometry) throws Exception {
//TODO: refacto avec au-dessus
        models.forEach(model -> {
            validateDates(model.getEndDate(), model.getStartDate());

            model.setObservationCollection(locationObservationCollectionURI);
            model.setFeatureOfInterest(featureOfInterest);
            model.setHasGeometry(hasGeometry);
        });

        if (models.size() >= 2) {
            validateConsistencyObservationList(models);
        }

        locationObservationDAO.create(session, models);
    }

    public LocationObservationModel getLocationObservationByURI(URI uri) throws NoSQLInvalidURIException {
        return locationObservationDAO.get(uri);
    }

    public LocationObservationModel getASpecificLocationObservation(URI collectionURI, Instant end, Instant start) throws NotAllowedException {
        List<LocationObservationModel> locations = locationObservationDAO.getSpecificLocation(collectionURI, end, start);

        if (locations.size() > 1) {
            throw new NotAllowedException("A feature of interest can't have 2 locations at the same time.");
        }

        return locations.get(0);
    }

    /**
     * @param collectionUriList   collection uris of observations list of features of interest
     * @param hasGeometry fetch only documents with a "geometry" field - displayable on a map
     * @param date        the date at which we search the location
     * @return list of the last locations of each feature of interest
     */
    public List<LocationObservationModel> getLastLocationObservation(List<URI> collectionUriList, boolean hasGeometry, Instant date, Geometry intersection) {
        LocationObservationSearchFilter searchFilter = new LocationObservationSearchFilter();

        searchFilter.setObservationCollectionList(collectionUriList);
        searchFilter.setHasGeometry(hasGeometry);

        if (Objects.nonNull(date)) {
            searchFilter.setEndDate(date);
        }
        if (Objects.nonNull(intersection)) {
            searchFilter.setIntersection(intersection);
        }

        ListWithPagination<LocationObservationModel> resultSearch = locationObservationDAO.searchWithPagination(searchFilter);

        // For each collection, get the latest location
        return new ArrayList<>(resultSearch.getList().stream()
                .collect(Collectors.groupingBy(
                        LocationObservationModel::getObservationCollection,
                        Collectors.collectingAndThen(
                                Collectors.maxBy(Comparator.comparing(LocationObservationModel::getEndDate)),
                                Optional::get)
                ))
                .values());
    }

    public ListWithPagination<LocationObservationModel> getLocationsHistory(
            URI collection,
            Instant startDate,
            Instant endDate,
            List<OrderBy> orderByList,
            Integer page,
            Integer pageSize) {

        Objects.requireNonNull(collection);

        LocationObservationSearchFilter searchFilter = new LocationObservationSearchFilter();

        searchFilter.setHasGeometry(false);
        searchFilter.setObservationCollection(collection);
        searchFilter.setStartDate(startDate);
        searchFilter.setEndDate(endDate);
        searchFilter.setHasGeometry(false);
        searchFilter.setOrderByList(orderByList);
        searchFilter.setPage(page);
        searchFilter.setPageSize(pageSize);

        return locationObservationDAO.searchWithPagination(searchFilter);
    }

    public List<LocationObservationModel> getIntersection(List<LocationObservationModel> locations, Geometry geometry) {
        LocationObservationSearchFilter filter = new LocationObservationSearchFilter();

        filter.setIncludedUris(locations.stream().map(LocationObservationModel::getUri).collect(Collectors.toList()));
        filter.setIntersection(geometry);

        return locationObservationDAO.searchWithPagination(filter).getList();
    }

    public void updateLocationObservation(ClientSession session, URI locationObservationCollectionURI, boolean hasGeometry, LocationModel locationModel) {
        try {
            LocationObservationModel locationObservationModel = locationObservationDAO.get(locationObservationCollectionURI);

            locationObservationModel.setLocation(locationModel);
            locationObservationModel.setHasGeometry(hasGeometry);

            locationObservationDAO.upsert(session, locationObservationModel);
        } catch (NoSQLInvalidURIException e) {
            throw new NotFoundURIException("Invalid or unknown URI ", locationObservationCollectionURI);
        }
    }

    public void updateASpecificLocationObservation(ClientSession session, LocationObservationModel existingObservation, LocationObservationModel newObservation) throws NoSQLInvalidURIException {
        locationObservationDAO.upsertSpecificLocation(session, existingObservation, newObservation);
    }

    /**
     * Use this method only if the object can have only one observation (e.g. sites)
     *
     * @param locationObservationCollectionURI collection URI
     * @throws NoSQLInvalidURIException
     */
    public void delete(ClientSession session, URI locationObservationCollectionURI) throws NoSQLInvalidURIException {
        locationObservationDAO.delete(session, locationObservationCollectionURI);
    }

    /**
     * Use this method to get a specific location observation for a feature of interest
     *
     * @param session mongo session
     * @param collectionURI location collection URI
     * @param end end date
     * @param start start date
     */
    public void deleteASpecificLocationObservation(ClientSession session, URI collectionURI, Instant end, Instant start) {
        locationObservationDAO.deleteSpecificLocation(session, collectionURI, end, start);
    }

    /**
     * Delete all location observations for a feature of interest
     *
     * @param locationObservationCollectionURI location observation
     */
    public void deleteLocationObservations(ClientSession session, URI locationObservationCollectionURI) {
        LocationObservationSearchFilter searchFilter = new LocationObservationSearchFilter();
        searchFilter.setObservationCollection(locationObservationCollectionURI);

        locationObservationDAO.deleteMany(session, searchFilter);
    }

    public int countLocationsForURI(URI locationObservationCollectionURI) {
        int count = 0;

        if (!Objects.isNull(locationObservationCollectionURI)) {
            LocationObservationSearchFilter searchFilter = new LocationObservationSearchFilter();
            searchFilter.setHasGeometry(false);
            searchFilter.setObservationCollection(locationObservationCollectionURI);

            count = (int) locationObservationDAO.count(searchFilter);
        }

        return count;
    }

    /**
     * Checks if a location has geometry, directly or indirectly through a facility (to):
     *
     * @param model location observation model
     * @param startDate start observation date of the geometry
     * @param endDate   end observation date of the geometry
     */
    public boolean checkHasGeometry(LocationObservationModel model, Instant startDate, Instant endDate){
        boolean hasGeometry = false;

        if(model.getLocation().getGeometry() != null){
            hasGeometry = true;
        } else if(model.getLocation().getTo() != null){
            LocationObservationSearchFilter searchFilter = new LocationObservationSearchFilter();
            searchFilter.setFeatureOfInterest(model.getLocation().getTo());
            searchFilter.setEndDate(endDate);
            searchFilter.setStartDate(startDate);
            searchFilter.setHasGeometry(true);

            ListWithPagination<LocationObservationModel> facilityLocationList = locationObservationDAO.searchWithPagination(searchFilter);
            if(!facilityLocationList.getList().isEmpty()){
                hasGeometry = true;
            }
        }

        return hasGeometry;
    }

    public void updateAssociatedLocationModel(ClientSession session, URI facility, URI collection) {
        //Get locations linked to the facility
        List<LocationObservationModel> locationToUpdateList = locationObservationDAO.searchLocationsWithGeomLinkedToFacility(facility);

        if (!locationToUpdateList.isEmpty()) {
            //Get facility locations
            LocationObservationSearchFilter filter = new LocationObservationSearchFilter();
            filter.setObservationCollection(collection);
            List<LocationObservationModel> facilityLocationList = locationObservationDAO.searchWithPagination(filter).getList();

            if (!facilityLocationList.isEmpty()) {
                if (facilityLocationList.size() == 1 && facilityLocationList.get(0).getEndDate() == null) { //Location from address (without date)
                    locationToUpdateList.forEach(loc -> loc.setHasGeometry(true));
                } else {
                    locationToUpdateList.forEach(loc -> {

                        List<LocationObservationModel> facilityLocationCorresponding = facilityLocationList.stream()
                                .filter(facilityLoc ->                      //filter location facility corresponding to with the dates of location to update
                                        facilityLoc.getEndDate().isBefore(loc.getEndDate()) ||
                                                facilityLoc.getEndDate().equals(loc.getEndDate()) ||
                                                (Objects.nonNull(facilityLoc.getStartDate()) ? (facilityLoc.getStartDate().isBefore(loc.getEndDate()) || facilityLoc.getEndDate().equals(loc.getEndDate())) : null))
                                .collect(Collectors.toList());

                        loc.setHasGeometry(!facilityLocationCorresponding.isEmpty());
                    });
                }
            } else {                         // no location
                locationToUpdateList.forEach(loc -> loc.setHasGeometry(false));
            }

            //update locations linked to facility
            locationToUpdateList.forEach(loc -> locationObservationDAO.upsertSpecificLocation(session, loc, loc));
        }
    }

    public LocationObservationModel getFacilityGeometry(LocationObservationModel location){
        LocationObservationModel facilityLocationCorresponding = new LocationObservationModel();

        LocationObservationSearchFilter searchFilter = new LocationObservationSearchFilter();
        searchFilter.setFeatureOfInterest(location.getLocation().getTo());
        searchFilter.setHasGeometry(true);
        searchFilter.setEndDate(location.getEndDate());
        searchFilter.setStartDate(location.getStartDate());

        ListWithPagination<LocationObservationModel> facilityLocationList = locationObservationDAO.searchWithPagination(searchFilter);

        if(!facilityLocationList.getList().isEmpty()){
            if (facilityLocationList.getList().size() == 1 && facilityLocationList.getList().get(0).getEndDate() == null) { //Location from address (without date)
                facilityLocationCorresponding= facilityLocationList.getList().get(0);
            } else {
                facilityLocationCorresponding = facilityLocationList.getList().stream()
                        .collect(Collectors.collectingAndThen(
                                Collectors.maxBy(Comparator.comparing(LocationObservationModel::getEndDate)),
                                Optional::get));
            }
        }

        location.getLocation().setGeometry(facilityLocationCorresponding.getLocation().getGeometry()) ;
        return location;
    }
    //#endregion

    //#region private
    /**
     * Checks if an object with location (not from an address) is valid :
     * - it must have one observation date;
     * - if there is a endDate, it must be after the "begin" date.
     *
     * @param startDate start observation date of the geometry
     * @param endDate   end observation date of the geometry
     * @throws NotAllowedException If dates are invalid
     */
    private void validateDates(Instant endDate, Instant startDate) throws NotAllowedException {
        if (Objects.isNull(endDate)) {
            throw new NotAllowedException("endDate cannot be null");
        }
        if (Objects.nonNull(startDate) && endDate.isBefore(endDate)) {
            throw new NotAllowedException("endDate (" + endDate + ") cannot be after startDate (" + startDate + ")");
        }
    }

    /**
     * Checks the consistency of all observation by feature of interest
     *
     * @param observations list of location observations
     */
    private void validateCollectionsConsistency(List<LocationObservationModel> observations){
        if(observations.size() >= 2){
            Map<URI,List<LocationObservationModel>> observationsByCollectionMap = observations.stream().collect(Collectors.groupingBy(LocationObservationModel::getObservationCollection));

            observationsByCollectionMap.forEach((collectionURI, groupedObservation) -> {
                //validate consistency the new observation list (observations) and between new and existing observations (existing observations)
                List<LocationObservationModel> existingObservations = getLocationsHistory(collectionURI, null, null, null,0,0).getList();
                List<LocationObservationModel> newAndExistingObservations = Stream.concat(groupedObservation.stream(), existingObservations.stream()).collect(Collectors.toList());

                if (newAndExistingObservations.size() >= 2) {
                    validateConsistencyObservationList(newAndExistingObservations);
                }
            });
        }
    }

    /**
     * Checks if the all observation dates are consistency
     * The object can't have 2 locations in the same time
     *
     * @param models list of location observations
     */
    private void validateConsistencyObservationList(List<LocationObservationModel> models) throws NotAllowedException {
        List<LocationObservationModel> modelsToCompare = new ArrayList<>(models);

        models.forEach(model -> {
            modelsToCompare.remove(model);

            NotAllowedException ex = new NotAllowedException(model.getObservationCollection().toString() + "can't be at 2 different locations in the same time (" + model.getEndDate() + ")");

            if (Objects.isNull(model.getStartDate())) {  // Instant
                modelsToCompare.forEach(m -> {
                    if (Objects.isNull(m.getStartDate())) { // Instant
                        if (m.getEndDate().equals(model.getEndDate())) {
                            throw ex;
                        }
                    } else { // Interval
                        if (model.getEndDate().isBefore(m.getEndDate()) && model.getEndDate().isAfter(m.getStartDate())) {
                            throw ex;
                        }
                    }
                });
            } else {  // Interval
                modelsToCompare.forEach(m -> {
                    if (Objects.isNull(m.getStartDate())) { // Instant
                        if (m.getEndDate().isBefore(model.getEndDate()) && m.getEndDate().isAfter(model.getStartDate())) {
                            throw ex;
                        }
                    } else { // Interval
                        if (!m.getEndDate().isBefore(model.getStartDate()) && !model.getEndDate().isBefore(m.getStartDate())) {
                            throw ex;
                        }
                    }
                });
            }
        });
    }
    //#endregion
}

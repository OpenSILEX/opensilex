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
import java.util.function.Function;
import java.util.stream.Collectors;

public class LocationObservationLogic {

    private final LocationObservationDAO locationObservationDAO;

    //#region constructor
    public LocationObservationLogic(MongoDBServiceV2 nosql) {
        this.locationObservationDAO = new LocationObservationDAO(nosql);
    }
    //#endregion

    //#region public
    public void createLocationObservation(ClientSession session, URI locationObservationCollectionURI, URI featureOfInterest, boolean hasGeometry, Instant startDate, Instant endDate, LocationModel locationModel) throws NoSQLAlreadyExistingUriException, URISyntaxException {
        LocationObservationModel locationObservationModel = new LocationObservationModel();

        locationObservationModel.setLocation(locationModel);

        if (Objects.nonNull(endDate)) {
            locationObservationModel.setEndDate(endDate);
            if (Objects.nonNull(startDate)) {
                locationObservationModel.setStartDate(startDate);
            }
        }
        locationObservationModel.setObservationCollection(locationObservationCollectionURI);
        locationObservationModel.setFeatureOfInterest(featureOfInterest);
        locationObservationModel.setHasGeometry(hasGeometry);

        locationObservationDAO.create(session, locationObservationModel);
    }

    public void createLocationObservations(ClientSession session, URI locationObservationCollectionURI, URI featureOfInterest, List<LocationObservationModel> models, boolean hasGeometry) throws Exception {

        models.forEach(model -> {
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

    /**
     * @param modelList   collections of observations list of features of interest
     * @param hasGeometry fetch only documents with a "geometry" field - displayable on a map
     * @param date        the date at which we search the location
     * @return list of the last locations of each feature of interest
     */
    public List<LocationObservationModel> getLastLocationObservation(List<LocationObservationCollectionModel> modelList, boolean hasGeometry, Instant date) {
        LocationObservationSearchFilter searchFilter = new LocationObservationSearchFilter();

        List<URI> uriList = modelList.stream().map(SPARQLResourceModel::getUri).collect(Collectors.toList());

        searchFilter.setObservationCollectionList(uriList);
        searchFilter.setHasGeometry(hasGeometry);

        if (Objects.nonNull(date)) {
            searchFilter.setEndDate(date);
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
        searchFilter.setOrderByList(orderByList);
        searchFilter.setPage(page);
        searchFilter.setPageSize(pageSize);

        if(pageSize == 0){
            searchFilter.setPageSize(10);
            int currentPage = 0;
            boolean doContinue = true;
            List<LocationObservationModel> finalList = new ArrayList<>();
            while(doContinue){
                searchFilter.setPage(currentPage);
                ListWithPagination<LocationObservationModel> nextPage = locationObservationDAO.searchWithPagination(searchFilter);
                finalList.addAll(nextPage.getList());
                doContinue = nextPage.getList().size() == 10;
                currentPage++;
            }
            return new ListWithPagination<>(finalList);
        }

        return locationObservationDAO.searchWithPagination(searchFilter);
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

    public void delete(ClientSession session, URI locationObservationCollectionURI) throws NoSQLInvalidURIException {
        locationObservationDAO.delete(session, locationObservationCollectionURI);
    }

    public void deleteLocationObservations(ClientSession session, URI locationObservationCollectionURI) {
        LocationObservationSearchFilter searchFilter = new LocationObservationSearchFilter();
        searchFilter.setObservationCollection(locationObservationCollectionURI);

        locationObservationDAO.deleteMany(session, searchFilter);
    }

    /**
     * Checks if an object with location (not from an address) is valid :
     * - it must have one observation date;
     * - if there is a endDate, it must be after the "begin" date.
     *
     * @param startDate start observation date of the geometry
     * @param endDate   end observation date of the geometry
     * @throws NotAllowedException If dates are invalid
     */
    public void validateDates(Instant endDate, Instant startDate) throws NotAllowedException {
        if (Objects.isNull(endDate)) {
            throw new NotAllowedException("endDate cannot be null");
        }
        if (Objects.nonNull(startDate) && endDate.isBefore(startDate)) {
            throw new NotAllowedException("endDate (" + endDate + ") cannot be after startDate (" + startDate + ")");
        }
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
     *
     * @param fromList list of models to create map with if they have a LocationObservationCollection
     * @param getLocationObservationCollectionFromModel pass a function to tell this method how to fetch LocationObservationCollection from a T
     * @param optionalEndDate Sites don't have a date, Facilities do
     * @return a map of T with or without corresponding location
     * @param <T> the type of the keys of the returned map
     */
    public <T> Map<T, LocationObservationModel> generateModelObservationCollectionMap(
            List<T> fromList,
            Function<T, LocationObservationCollectionModel> getLocationObservationCollectionFromModel,
            Instant optionalEndDate
    ){
        Map<T, LocationObservationModel> result = new HashMap<>();

        //Get models with locations
        Map<T, LocationObservationCollectionModel> modelsWithLocationMap = fromList.stream()
                .filter(model -> getLocationObservationCollectionFromModel.apply(model) != null)
                .collect(Collectors.toMap(Function.identity(), getLocationObservationCollectionFromModel));

        if (!modelsWithLocationMap.isEmpty()) {
            List<LocationObservationModel> locationObservationModels = getLastLocationObservation(
                    new ArrayList<>(modelsWithLocationMap.values()),
                    true,
                    optionalEndDate
            );

            var locationObservationMap = locationObservationModels.stream()
                    .collect(Collectors.toMap(LocationObservationModel::getObservationCollection, Function.identity()));

            modelsWithLocationMap.forEach((model, collection) -> {
                var observation = locationObservationMap.get(collection.getUri());
                if (Objects.nonNull(observation)) {
                    result.put(model, observation);
                }
            });

        }
        return result;
    }

    //#endregion

    //#region private

    /**
     * Checks consistency of all observation dates
     * The object can't have 2 locations in the same time
     *
     * @param models list of location observations
     */
    private void validateConsistencyObservationList(List<LocationObservationModel> models) throws NotAllowedException {
        List<LocationObservationModel> modelsToCompare = new ArrayList<>(models);

        models.forEach(model -> {
            modelsToCompare.remove(model);

            if (Objects.isNull(model.getStartDate())) {  // Instant
                modelsToCompare.forEach(m -> {
                    if (Objects.isNull(m.getStartDate())) { // Instant
                        if (m.getEndDate().equals(model.getEndDate())) {
                            notAllowedException(model);
                        }
                    } else { // Interval
                        if (model.getEndDate().isBefore(m.getEndDate()) && model.getEndDate().isAfter(m.getStartDate())) {
                            notAllowedException(model);
                        }
                    }
                });
            } else {  // Interval
                modelsToCompare.forEach(m -> {
                    if (Objects.isNull(m.getStartDate())) { // Instant
                        if (m.getEndDate().isBefore(model.getEndDate()) && m.getEndDate().isAfter(model.getStartDate())) {
                            notAllowedException(model);
                        }
                    } else { // Interval
                        if (!m.getEndDate().isBefore(model.getStartDate()) && !model.getEndDate().isBefore(m.getStartDate())) {
                            notAllowedException(model);
                        }
                    }
                });
            }
        });
    }

    private void notAllowedException(LocationObservationModel model) throws NotAllowedException {
        StringBuilder message = new StringBuilder();
        message.append(model.getObservationCollection().toString())
                .append("can't be at 2 different locations in the same time (")
                .append(model.getEndDate().toString())
                .append(")");

        throw new NotAllowedException(message.toString());
    }
    //#endregion
}

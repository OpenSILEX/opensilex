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
import com.mongodb.client.model.geojson.Geometry;
import org.opensilex.core.location.dal.LocationObservationCollectionModel;
import org.opensilex.core.location.dal.LocationObservationSearchFilter;
import org.opensilex.nosql.exceptions.NoSQLAlreadyExistingUriException;
import org.opensilex.nosql.exceptions.NoSQLInvalidURIException;
import org.opensilex.nosql.mongodb.service.v2.MongoDBServiceV2;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.server.exceptions.NotFoundURIException;

import javax.ws.rs.NotAllowedException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class LocationObservationLogic {

    private final LocationObservationDAO locationObservationDAO;

    //#region constructor
    public LocationObservationLogic(MongoDBServiceV2 nosql) {
        this.locationObservationDAO = new LocationObservationDAO(nosql);
    }
    //#endregion

    //#region public
    public void createLocationObservation (ClientSession session, URI locationObservationCollectionURI, URI featureOfInterest, boolean hasGeometry, Instant date, Instant endDate, LocationModel locationModel) throws NoSQLAlreadyExistingUriException, URISyntaxException {
        LocationObservationModel locationObservationModel = new LocationObservationModel();

        locationObservationModel.setLocation(locationModel);

        if(date != null) {
            locationObservationModel.setDate(date);
            if(endDate != null) {
                locationObservationModel.setEndDate(endDate);
            }
        }
        locationObservationModel.setObservationCollection(locationObservationCollectionURI);
        locationObservationModel.setFeatureOfInterest(featureOfInterest);
        locationObservationModel.setHasGeometry(hasGeometry);

        locationObservationDAO.create(session, locationObservationModel);
    }

    public void createLocationObservations(ClientSession session, URI locationObservationCollectionURI, URI featureOfInterest, List<LocationObservationModel> models, boolean hasGeometry ) throws NoSQLAlreadyExistingUriException, URISyntaxException {

        models.forEach(model -> {
            validateDates(model.getDate(), model.getEndDate());

            model.setObservationCollection(locationObservationCollectionURI);
            model.setFeatureOfInterest(featureOfInterest);
            model.setHasGeometry(hasGeometry);
        });

        if(models.size() >= 2 ) {
            validateConsistencyObservationList(models);
        }

        locationObservationDAO.create(session, models);
    }

    public LocationObservationModel getLocationObservationByURI(URI uri) throws NoSQLInvalidURIException {
        return locationObservationDAO.get(uri);
    }

    /**
     * @param modelList collections of observations list of features of interest
     * @param hasGeometry fetch only documents with a "geometry" field - displayable on a map
     * @param date the date at which we search the location
     * @param intersection geographical limits of locations
     * @return list of the last locations of each feature of interest
     */
    public List<LocationObservationModel> getLastLocationObservation(List<LocationObservationCollectionModel> modelList, boolean hasGeometry, Instant date, Geometry intersection) {
        //TODO: get last location for elements with date linked to location - not implement for the moment (OS, facilities,...). Site are a particular case: one location without linked date
        LocationObservationSearchFilter searchFilter = new LocationObservationSearchFilter();
        List<LocationObservationModel> resultSearch;

        List<URI> uriList = modelList.stream().map(SPARQLResourceModel::getUri).collect(Collectors.toList());

        searchFilter.setObservationCollectionList(uriList);
        searchFilter.setHasGeometry(hasGeometry);

        if(date != null) {
            searchFilter.setDate(date);
        }

        resultSearch = locationObservationDAO.searchWithPagination(searchFilter).getList();

        return resultSearch;
    }

    public void updateLocationObservation(ClientSession session, URI locationObservationCollectionURI, boolean hasGeometry,LocationModel locationModel) {
        try {
            LocationObservationModel locationObservationModel = locationObservationDAO.get(locationObservationCollectionURI);

            locationObservationModel.setLocation(locationModel);
            locationObservationModel.setHasGeometry(hasGeometry);

            locationObservationDAO.upsert(session, locationObservationModel);
        } catch (NoSQLInvalidURIException e) {
            throw new NotFoundURIException("Invalid or unknown data URI ", locationObservationCollectionURI);
        }
    }

    public void delete(ClientSession session, URI locationObservationCollectionURI) throws NoSQLInvalidURIException {
        locationObservationDAO.delete(session, locationObservationCollectionURI);
    }
    //#endregion

    //#region private
    /**
     * Checks if an object with location (not from an address) is valid :
     *     - it must have one observation date;
     *     - if there is a endDate, it must be after the "begin" date.
     *
     *
     * @param date observation date of the geometry
     * @param endDate end observation date of the geometry
     * @throws NotAllowedException If dates are invalid
     */
    private void validateDates(Instant date, Instant endDate){
        if(Objects.isNull(date)){
            throw new NotAllowedException("Date cannot be null");
        }
        if(!Objects.isNull(endDate) && endDate.isBefore(date)){
            throw new NotAllowedException("Date ("+ date +") cannot be after endDate ("+ endDate + ")");
        }
    }

    /**
     * Checks if the all observation dates are consistency
     * The object can't have 2 locations in the same time
     *
     * @param models list of location observations
     */
    private void validateConsistencyObservationList(List<LocationObservationModel> models){
        //TODO : a optimiser - stream ? boucle while models.size() <2 ? refactoring?
        //TODO: message d'erreur lisible
            List<LocationObservationModel> modelsToCompare = new ArrayList<>(models);

            models.forEach(model -> {
                modelsToCompare.remove(model);

                NotAllowedException ex = new NotAllowedException( model.getObservationCollection().toString() + "can't be at 2 different locations in the same time (" + model.getDate() + ")");

                if(Objects.isNull(model.getEndDate())){  // Instant
                    modelsToCompare.forEach(m -> {
                        if(Objects.isNull(m.getEndDate())){
                           if(m.getDate().equals(model.getDate())){
                               throw ex;
                           }
                        } else {
                            if(model.getDate().isBefore(m.getEndDate()) && model.getDate().isAfter(m.getDate())){
                                throw ex;
                            }
                        }
                    });
                } else {  // Interval
                    modelsToCompare.forEach(m -> {
                        if(Objects.isNull(m.getEndDate())){
                            if(m.getDate().isBefore(model.getEndDate()) && m.getDate().isAfter(model.getDate())){
                                throw ex;
                            }
                        } else {
                            if(!m.getEndDate().isBefore(model.getDate()) && !model.getEndDate().isBefore(m.getDate())){
                                throw ex;
                            }
                        }
                    });
                }
            });
    }
    //#endregion
}

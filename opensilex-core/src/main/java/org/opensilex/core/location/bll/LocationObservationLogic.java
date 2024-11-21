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

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

public class LocationObservationLogic {

    private final LocationObservationDAO locationObservationDAO;

    //#region constructor
    public LocationObservationLogic(MongoDBServiceV2 nosql) {
        this.locationObservationDAO = new LocationObservationDAO(nosql);
    }
    //#endregion

    //#region public
    public void createLocationObservation (ClientSession session, URI locationObservationCollectionURI, URI featureOfInterest, boolean hasGeometry, LocationModel locationModel) throws NoSQLAlreadyExistingUriException, URISyntaxException {
        LocationObservationModel locationObservationModel = new LocationObservationModel();

        locationObservationModel.setLocation(locationModel);
        locationObservationModel.setObservationCollection(locationObservationCollectionURI);
        locationObservationModel.setFeatureOfInterest(featureOfInterest);
        locationObservationModel.setHasGeometry(hasGeometry);

        locationObservationDAO.create(session, locationObservationModel);
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

    public void updateLocationObservation(ClientSession session, URI locationObservationCollectionURI, boolean hasGeometry,LocationModel locationModel) throws NoSQLInvalidURIException {
        LocationObservationModel locationObservationModel = locationObservationDAO.get(locationObservationCollectionURI);

        locationObservationModel.setLocation(locationModel);
        locationObservationModel.setHasGeometry(hasGeometry);

        locationObservationDAO.upsert(session, locationObservationModel);
    }

    public void delete(ClientSession session, URI locationObservationCollectionURI) throws NoSQLInvalidURIException {
        locationObservationDAO.delete(session, locationObservationCollectionURI);
    }
    //#endregion

    //#region private
    //#endregion
}

/*
 * *****************************************************************************
 *                         LocationObservationCollectionLogic.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2024.
 * Last Modification: 26/07/2024 13:35
 * Contact: alexia.chiavarino@inrae.fr
 * *****************************************************************************
 *
 */

package org.opensilex.core.location.bll;

import org.opensilex.core.location.dal.LocationObservationCollectionDAO;
import org.opensilex.core.location.dal.LocationObservationCollectionModel;
import org.opensilex.server.exceptions.BadRequestException;
import org.opensilex.server.exceptions.NotFoundURIException;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;
import org.opensilex.sparql.service.SPARQLResult;
import org.opensilex.sparql.service.SPARQLService;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class LocationObservationCollectionLogic {
    private final SPARQLService sparql;

    private final LocationObservationCollectionDAO locationObservationCollectionDAO;

    //#region constructor
    public LocationObservationCollectionLogic(SPARQLService sparqlService) {
        this.sparql = sparqlService;

        this.locationObservationCollectionDAO = new LocationObservationCollectionDAO(sparql);
    }
    //#endregion

    //#region public
    public URI createLocationObservationCollection(URI featureOfInterest) throws Exception {
        checkUniqueObservationCollection(Collections.singletonList(featureOfInterest));
        LocationObservationCollectionModel locationObservationCollectionModel = new LocationObservationCollectionModel(featureOfInterest);
        return locationObservationCollectionDAO.create(locationObservationCollectionModel);
    }

    /**
     * DOES NOT CURRENTLY CHECK UNIQUENESS
     *
     * @param featuresOfInterest the URIs for whom we want to create a new LocObsCollection for
     * @return the newly created Collections with their URIs filled
     */
    public List<LocationObservationCollectionModel> createList(List<URI> featuresOfInterest) throws Exception {
        checkUniqueObservationCollection(featuresOfInterest);
        List<LocationObservationCollectionModel> collectionModels = featuresOfInterest
                .stream()
                .map(LocationObservationCollectionModel::new).toList();
        locationObservationCollectionDAO.createList(collectionModels);
        return collectionModels;
    }

    public URI getLocationObservationCollectionURI(URI featureOfInterest) {
        try {
            return locationObservationCollectionDAO.getCollectionURI(featureOfInterest);
        } catch (Exception e) {
            throw new NotFoundURIException("No location collection found for this URI", featureOfInterest);
        }
    }

    /**
     *
     * @param featureOfInterests the uris of elements we want LocationCollections for
     * @return the LocationCollection uri of each featureOfInterest, in a Map of FeatureOfInterestURI => LocationCollectionURI
     */
    public Map<URI, URI> getLocationObservationCollectionPerFeatureOfInterest(List<URI> featureOfInterests) throws SPARQLException {
        return locationObservationCollectionDAO.getCollections(featureOfInterests);
    }

    /**
     * @param rdfType type of object
     * @return for each object of the rdfType, return the object uri, rdfType, and the location observation collection linked
     */
    public Map<SPARQLNamedResourceModel, LocationObservationCollectionModel> getLocationObservationCollectionListByType(URI rdfType) throws SPARQLException {
        return locationObservationCollectionDAO.getCollectionByType(rdfType);
    }

    public void deleteLocationObservationCollection(URI collectionURI) throws Exception {
        locationObservationCollectionDAO.delete(collectionURI);
    }

    public void deleteMany(List<URI> collectionUris) throws Exception {
        locationObservationCollectionDAO.deleteMany(collectionUris);
    }
    //#endregion

    //#region private
    private void checkUniqueObservationCollection(List<URI> featuresOfInterest) {
        try {
            List<SPARQLResult> result = locationObservationCollectionDAO.validateUniquenessObservationCollection(featuresOfInterest);

            if (result.size() > 1) {
                throw new BadRequestException("Invalid observation collection model : each observation collection must be unique for each feature of interest");
            }
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error when checking location collection", e);
        }
    }
    //#endregion

}

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
import org.opensilex.sparql.service.SPARQLResult;
import org.opensilex.sparql.service.SPARQLService;

import java.net.URI;
import java.util.List;

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
        checkUniqueObservationCollection(featureOfInterest);

        LocationObservationCollectionModel locationObservationCollectionModel = new LocationObservationCollectionModel();

        locationObservationCollectionModel.setFeatureOfInterest(featureOfInterest);

        return locationObservationCollectionDAO.create(locationObservationCollectionModel);
    }

    public URI getLocationObservationCollectionURI(URI featureOfInterest) {
        try {
            return locationObservationCollectionDAO.getCollectionURI(featureOfInterest);
        } catch (Exception e) {
            throw new NotFoundURIException("No location collection found for this URI", featureOfInterest);
        }
    }

    public void deleteLocationObservationCollection(URI collectionURI) throws Exception {
        locationObservationCollectionDAO.delete(collectionURI);
    }
    //#endregion

    //#region private
    private void checkUniqueObservationCollection(URI featureOfInterest) {
        try {
            List<SPARQLResult> result = locationObservationCollectionDAO.validateUniquenessObservationCollection(featureOfInterest);

            if (result.size() > 1) {
                throw new BadRequestException("Invalid observation collection model : observation collection must be unique for each feature of interest");
            }
        } catch (SPARQLException e) {
            throw new RuntimeException("Unexpected error when checking location collection", e);
        }
    }
    //#endregion

}

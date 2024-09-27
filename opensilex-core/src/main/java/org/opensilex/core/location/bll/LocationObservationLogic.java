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
import org.opensilex.nosql.exceptions.NoSQLAlreadyExistingUriException;
import org.opensilex.nosql.exceptions.NoSQLInvalidURIException;
import org.opensilex.nosql.mongodb.service.v2.MongoDBServiceV2;
import org.opensilex.server.exceptions.NotFoundURIException;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;

public class LocationObservationLogic {

    private final LocationObservationDAO locationObservationDAO;

    //#region constructor
    public LocationObservationLogic(MongoDBServiceV2 nosql) {
        this.locationObservationDAO = new LocationObservationDAO(nosql);
    }
    //#endregion

    //#region public
    public void createLocationObservation (ClientSession session, URI locationObservationCollectionURI, boolean hasGeometry, Instant date, Instant endDate, LocationModel locationModel) throws NoSQLAlreadyExistingUriException, URISyntaxException {
        LocationObservationModel locationObservationModel = new LocationObservationModel();

        locationObservationModel.setLocation(locationModel);

        if(date != null) {
            locationObservationModel.setDate(date);
            if(endDate != null) {
                locationObservationModel.setEndDate(endDate);
            }
        }
        locationObservationModel.setObservationCollection(locationObservationCollectionURI);
        locationObservationModel.setHasGeometry(hasGeometry);

        locationObservationDAO.create(session, locationObservationModel);
    }

    public LocationObservationModel getLocationObservationByURI(URI uri) throws NoSQLInvalidURIException {
        return locationObservationDAO.get(uri);
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
    //#endregion
}

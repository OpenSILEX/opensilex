/*
 * *****************************************************************************
 *                         LocationObservationDTO.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2024.
 * Last Modification: 02/10/2024 11:11
 * Contact: alexia.chiavarino@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr,
 *
 *
 * *****************************************************************************
 *
 */

package org.opensilex.core.location.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.geojson.GeoJsonObject;
import org.opensilex.core.location.bll.LocationLogic;
import org.opensilex.core.location.dal.LocationModel;
import org.opensilex.core.location.dal.LocationObservationModel;

import java.time.Instant;

public class LocationObservationDTO {

    protected GeoJsonObject geojson;

    protected Instant date;

    protected Instant endDate;

    public GeoJsonObject getGeojson() {
        return geojson;
    }

    public void setGeojson(GeoJsonObject geojson) {
        this.geojson = geojson;
    }

    public Instant getDate() {
        return date;
    }

    public void setDate(Instant date) {
        this.date = date;
    }

    public Instant getEndDate() {
        return endDate;
    }

    public void setEndDate(Instant endDate) {
        this.endDate = endDate;
    }

    public void toModel(LocationObservationModel model) {
        try {
            LocationModel location = LocationLogic.buildLocationModel(LocationLogic.geoJsonToGeometry(geojson), null, null, null, null);
            model.setLocation(location);
        } catch (JsonProcessingException e){
            throw new RuntimeException(e);
        }

        if (getDate() != null) {
            model.setDate(getDate());
        }
        if (getEndDate() != null) {
            model.setEndDate(getEndDate());
        }
    }

    public LocationObservationModel newModel() {
        LocationObservationModel instance = new LocationObservationModel();
        toModel(instance);

        return instance;
    }
}

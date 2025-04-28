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
import java.util.Objects;

public class LocationObservationDTO {

    private GeoJsonObject geojson;

    private Instant startDate;

    private Instant endDate;

    public GeoJsonObject getGeojson() {
        return geojson;
    }

    public void setGeojson(GeoJsonObject geojson) {
        this.geojson = geojson;
    }

    public Instant getStartDate() {
        return startDate;
    }

    public void setStartDate(Instant startDate) {
        this.startDate = startDate;
    }

    public Instant getEndDate() {
        return endDate;
    }

    public void setEndDate(Instant endDate) {
        this.endDate = endDate;
    }

    private void toModel(LocationObservationModel model) {
        try {
            LocationModel location = LocationLogic.buildLocationModel(LocationLogic.geoJsonToGeometry(geojson), null, null, null, null);
            model.setLocation(location);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        if (Objects.nonNull(getStartDate())) {
            model.setStartDate(getStartDate());
        }
        if (Objects.nonNull(getEndDate())) {
            model.setEndDate(getEndDate());
        }
    }

    public LocationObservationModel newModel() {
        LocationObservationModel instance = new LocationObservationModel();
        toModel(instance);

        return instance;
    }

    private void fromModel(LocationObservationModel model) {
        if (Objects.nonNull(model.getLocation())) {
            try {
                setGeojson(LocationLogic.geometryToGeoJson(model.getLocation().getGeometry()));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        if (Objects.nonNull(model.getStartDate())) {
            setStartDate(model.getStartDate());
        }
        if (Objects.nonNull(model.getEndDate())) {
            setEndDate(model.getEndDate());
        }
    }

    public static LocationObservationDTO getDTOFromModel(LocationObservationModel model) {
        LocationObservationDTO dto = new LocationObservationDTO();
        dto.fromModel(model);

        return dto;
    }
}

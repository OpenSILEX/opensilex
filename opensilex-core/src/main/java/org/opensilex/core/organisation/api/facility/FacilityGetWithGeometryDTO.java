/*
 * *****************************************************************************
 *                         FacilityGetWithGeometryDTO.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2024.
 * Last Modification: 22/10/2024 13:48
 * Contact: alexia.chiavarino@inrae.fr
 * *****************************************************************************
 *
 */

package org.opensilex.core.organisation.api.facility;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.geojson.GeoJsonObject;
import org.opensilex.core.location.bll.LocationLogic;
import org.opensilex.core.location.dal.LocationObservationModel;
import org.opensilex.core.organisation.dal.facility.FacilityModel;
import org.opensilex.server.exceptions.BadRequestException;

import java.util.Objects;

public class FacilityGetWithGeometryDTO extends FacilityDTO {

    @JsonProperty("geometry")
    protected GeoJsonObject geometry;

    public GeoJsonObject getGeometry() {
        return geometry;
    }

    public void setGeometry(GeoJsonObject geometry) {
        this.geometry = geometry;
    }

    public void fromModel(FacilityModel model, LocationObservationModel location) {
        super.fromModel(model);

        if (Objects.nonNull(location)) {
            try {
                setGeometry(LocationLogic.geometryToGeoJson(location.getLocation().getGeometry()));
            } catch (JsonProcessingException e) {
                throw new BadRequestException("Could not read GEOJSON geometry");
            }
        }
    }
}
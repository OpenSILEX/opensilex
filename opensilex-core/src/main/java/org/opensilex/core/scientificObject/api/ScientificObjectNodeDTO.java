/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.scientificObject.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.geojson.GeoJsonObject;
import org.opensilex.core.location.api.LocationObservationDTO;
import org.opensilex.core.location.dal.LocationObservationModel;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import org.opensilex.sparql.response.NamedResourceDTO;

import java.time.LocalDate;
import java.util.Objects;


/**
 *
 * @author vmigot
 */
public class ScientificObjectNodeDTO extends NamedResourceDTO<ScientificObjectModel> {

    private LocationObservationDTO location;

    @Deprecated
    @ApiModelProperty(notes = "Object geometry. Depreciated : use location instead")
    private GeoJsonObject geometry;

    @JsonProperty("creation_date")
    @ApiModelProperty(value = "Scientific object creation date")
    private LocalDate creationDate;

    @JsonProperty("destruction_date")
    @ApiModelProperty(value = "Scientific object creation date")
    private LocalDate destructionDate;

    public LocationObservationDTO getLocation() {
        return location;
    }

    public void setLocation(LocationObservationDTO location) {
        this.location = location;
    }

    @Deprecated
    public GeoJsonObject getGeometry() {
        return geometry;
    }

    @Deprecated
    public void setGeometry(GeoJsonObject geometry) {
        this.geometry = geometry;
    }

    public LocalDate getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDate creationDate) {
        this.creationDate = creationDate;
    }

    public LocalDate getDestructionDate() {
        return destructionDate;
    }

    public void setDestructionDate(LocalDate destructionDate) {
        this.destructionDate = destructionDate;
    }

    @Override
    public ScientificObjectModel newModelInstance() {
        return new ScientificObjectModel();
    }

    public static ScientificObjectNodeDTO getDTOFromModel(ScientificObjectModel model) {
        ScientificObjectNodeDTO dto = new ScientificObjectNodeDTO();
        dto.fromModel(model);
        dto.setCreationDate(model.getCreationDate());
        dto.setDestructionDate(model.getDestructionDate());

        return dto;
    }

    public static ScientificObjectNodeDTO getDTOFromModel(ScientificObjectModel model, LocationObservationModel location) {
        ScientificObjectNodeDTO dto = getDTOFromModel(model);
        if (Objects.nonNull(location)) {
            dto.setLocation(LocationObservationDTO.getDTOFromModel(location));
            dto.setGeometry(dto.getLocation().getGeojson());
        }

        return dto;
    }
}

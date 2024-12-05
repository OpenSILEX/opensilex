/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.scientificObject.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.mongodb.client.model.geojson.Geometry;
import io.swagger.annotations.ApiModelProperty;
import org.geojson.GeoJsonObject;
import org.opensilex.core.geospatial.dal.GeospatialModel;
import org.opensilex.core.location.api.LocationObservationDTO;
import org.opensilex.core.location.dal.LocationObservationModel;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import org.opensilex.sparql.response.NamedResourceDTO;

import java.time.LocalDate;

import static org.opensilex.core.geospatial.dal.GeospatialDAO.geometryToGeoJson;

/**
 *
 * @author vmigot
 */
public class ScientificObjectNodeDTO extends NamedResourceDTO<ScientificObjectModel> {

    //TODO: à supprimer
    private GeoJsonObject geometry;

    private LocationObservationDTO location;

    @JsonProperty("creation_date")
    @ApiModelProperty(value = "Scientific object creation date")
    private LocalDate creationDate;

    @JsonProperty("destruction_date")
    @ApiModelProperty(value = "Scientific object creation date")
    private LocalDate destructionDate;

    public GeoJsonObject getGeometry() {
        return geometry;
    }

    public void setGeometry(GeoJsonObject geometry) {
        this.geometry = geometry;
    }

    public LocationObservationDTO getLocation() {
        return location;
    }

    public void setLocation(LocationObservationDTO location) {
        this.location = location;
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

    public static ScientificObjectNodeDTO getDTOFromModel(ScientificObjectModel model, Geometry geometryByURI, LocationObservationModel location) {
        ScientificObjectNodeDTO dto = getDTOFromModel(model);
        //TODO: à supprimer
        if (geometryByURI != null) {
            try {
                dto.setGeometry(geometryToGeoJson(geometryByURI));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
        if (location != null) {
            dto.setLocation(LocationObservationDTO.getDTOFromModel(location));
        }
        return dto;
    }

    public static ScientificObjectNodeDTO getDTOFromModel(GeospatialModel geospatialModel) {
        ScientificObjectNodeDTO dto = new ScientificObjectNodeDTO();
        //TODO: à supprimer
        if (geospatialModel != null) {
            try {
                dto.setType(geospatialModel.getRdfType());
                dto.setUri(geospatialModel.getUri());
                dto.setName(geospatialModel.getName());
                dto.setGeometry(geometryToGeoJson(geospatialModel.getGeometry()));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
        return dto;
    }
}

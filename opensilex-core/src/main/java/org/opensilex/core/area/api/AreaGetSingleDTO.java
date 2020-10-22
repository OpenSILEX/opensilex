/*
 * *******************************************************************************
 *                     AreaGetSingleDTO.java
 * OpenSILEX
 * Copyright © INRAE 2020
 * Creation date: September 17, 2020
 * Contact: vincent.migot@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 * *******************************************************************************
 */

package org.opensilex.core.area.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.geojson.GeoJsonObject;
import org.opensilex.core.area.dal.AreaModel;
import org.opensilex.core.geospatial.dal.GeospatialModel;

import javax.validation.constraints.NotNull;
import java.net.URI;

import static org.opensilex.core.geospatial.dal.GeospatialDAO.geometryToGeoJson;

/**
 * DTO representing JSON for the search area or obtain them by uri
 *
 * @author Jean Philippe VERT
 */
public class AreaGetSingleDTO {
    /**
     * Area URI
     */
    protected URI uri;

    /**
     * Area Variety URI
     */
    protected String name;

    /**
     * Area Type : Area, WindyArea, etc
     */
    protected URI type;

    /**
     * geometry of the Area
     */
    protected GeoJsonObject geometry;

    /**
     * comment
     */
    protected String comment;

    /**
     * Convert Area Model into Area DTO
     *
     * @param model         Area Model to convert
     * @param geometryByURI Geometry Model to convert
     * @return Corresponding user DTO
     */
    @NotNull
    public static AreaGetSingleDTO fromModel(@NotNull AreaModel model, GeospatialModel geometryByURI) throws JsonProcessingException {
        AreaGetSingleDTO dto = new AreaGetSingleDTO();

        dto.setUri(model.getUri());
        dto.setName(model.getName());
        dto.setType(model.getType());

        if (geometryByURI.getGeometry() != null) {
            dto.setGeometry(geometryToGeoJson(geometryByURI.getGeometry()));
        }

        if (model.getDescription() != null) {
            dto.setComment(model.getDescription());
        }

        return dto;
    }

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public URI getType() {
        return type;
    }

    public void setType(URI type) {
        this.type = type;
    }

    public GeoJsonObject getGeometry() {
        return geometry;
    }

    public void setGeometry(GeoJsonObject geometry) {
        this.geometry = geometry;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
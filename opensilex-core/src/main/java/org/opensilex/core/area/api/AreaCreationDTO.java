/*
 * *******************************************************************************
 *                     AreaCreationDTO.java
 * OpenSILEX
 * Copyright © INRAE 2020
 * Creation date: September 14, 2020
 * Contact: vincent.migot@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 * *******************************************************************************
 */
package org.opensilex.core.area.api;

import io.swagger.annotations.ApiModelProperty;
import org.geojson.GeoJsonObject;
import org.opensilex.core.area.dal.AreaModel;
import org.opensilex.server.rest.validation.Required;
import org.opensilex.server.rest.validation.ValidURI;

import javax.validation.constraints.NotNull;
import java.net.URI;

/**
 * DTO representing JSON for area shipping
 *
 * @author Jean Philippe VERT
 */
public class AreaCreationDTO {
    /**
     * Area URI
     */
    @ValidURI
    @ApiModelProperty(value = "Area URI", example = "http://opensilex/set/area/Z_001")
    protected URI uri;

    /**
     * Area name
     */
    @Required
    @ApiModelProperty(value = "Area name", example = "Z_001", required = true)
    protected String name;

    /**
     * Area Type : Area, WindyArea, etc
     */
    @NotNull
    @ApiModelProperty(value = "type URI", example = "vocabulary:WindyArea")
    protected URI type;

    /**
     * geometry of the Area
     */
    @NotNull
    @ApiModelProperty(value = "The geographical coordinates of the area", example = "{'type':'Polygon','coordinates':[[[3.97167246,43.61328981], [3.97171243,43.61332417],[3.9717427,43.61330558],[3.97170272,43.61327122], [3.97167246,43.61328981],[3.97167246,43.61328981]]]}")
    protected GeoJsonObject geometry;

    /**
     * description
     */
    @ApiModelProperty(value = "comment")
    protected String description;

    public URI getUri() {
        return uri;
    }

    public AreaCreationDTO setUri(URI uri) {
        this.uri = uri;
        return this;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public AreaModel newModel() {
        AreaModel model = new AreaModel();

        if (uri != null) {
            model.setUri(uri);
        }

        if (name != null) {
            model.setName(name);
        }

        if (type != null) {
            model.setType(type);
        }

        if (geometry != null) {
            model.setGeometry(geometry);
        }

        if (description != null) {
            model.setDescription(description);
        }

        return model;
    }
}

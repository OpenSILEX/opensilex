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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import org.geojson.GeoJsonObject;
import org.opensilex.core.event.api.EventCreationDTO;
import org.opensilex.server.rest.validation.Required;
import org.opensilex.server.rest.validation.ValidURI;
import javax.validation.constraints.NotNull;
import java.net.URI;

/**
 * DTO representing JSON for area shipping
 *
 * @author Jean Philippe VERT
 */
@Schema
@JsonPropertyOrder({"uri", "is_structural_area", "rdf_type", "name", "description", "geometry","event"})
public class AreaCreationDTO {
    /**
     * Area URI
     */
    @ValidURI
    @Schema(description = "Area URI", example = "http://opensilex/set/area/Z_001")
    protected URI uri;

    /**
     * Area name
     */
    @Required
    @Schema(description = "Area name", example = "Z_001", requiredMode = Schema.RequiredMode.REQUIRED)
    protected String name;

    /**
     * Area Type : Area, WindyArea, etc
     */
    @NotNull
    @JsonProperty("rdf_type")
    @Schema(description = "Area rdf_type", requiredMode = Schema.RequiredMode.REQUIRED, name = "rdf_type", example = "vocabulary:WindyArea")
    protected URI rdfType;

    /**
     * geometry of the Area
     */
    @NotNull
    @Schema(description = "The geographical coordinates of the area", requiredMode = Schema.RequiredMode.REQUIRED)
    protected GeoJsonObject geometry;

    /**
     * description
     */
    @Schema(description = "Description of the area", example = "Protocol n°1289 - Amount of water 5 ml/Days.")
    protected String description;

    /**
     * subClass Area Type : Temporal or Structural
     */
    @NotNull
    @JsonProperty("is_structural_area")
    @Schema(description = "Area type ( true = structural | false = temporal)", required = true)
    protected Boolean isStructuralArea;

    /**
     * Event created and linked to the temporal area
     */
    @Schema(description = "Event linked to the area")
    protected EventCreationDTO event;

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

    public URI getRdfType() {
        return rdfType;
    }

    public void setRdfType(URI rdfType) {
        this.rdfType = rdfType;
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

    public Boolean getIsStructuralArea() {
        return isStructuralArea;
    }

    public void setIsStructuralArea(Boolean structuralArea) {
        isStructuralArea = structuralArea;
    }

    public EventCreationDTO getEvent() {
        return event;
    }

    public void setEvent(EventCreationDTO event) {
        this.event = event;
    }
}

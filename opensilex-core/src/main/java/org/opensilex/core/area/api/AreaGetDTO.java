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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.geojson.GeoJsonObject;
import org.opensilex.core.area.dal.AreaModel;
import org.opensilex.core.geospatial.dal.GeospatialModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;

import java.net.URI;
import java.net.URISyntaxException;

import static org.opensilex.core.geospatial.dal.GeospatialDAO.geometryToGeoJson;

/**
 * DTO representing JSON for the search area or obtain them by uri
 *
 * @author Jean Philippe VERT
 */
@JsonPropertyOrder({"uri", "rdf_type", "name", "description", "author", "geometry"})
public class AreaGetDTO {
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
    @JsonProperty("rdf_type")
    protected URI rdfType;

    /**
     * geometry of the Area
     */
    protected GeoJsonObject geometry;

    /**
     * description
     */
    protected String description;

    /**
     * author
     */
    protected URI author;

    /**
     * Convert Area Model into Area DTO
     *
     * @param model         Area Model to convert
     * @param geometryByURI Geometry Model to convert
     * @return Corresponding user DTO
     */
    public static AreaGetDTO fromModel(AreaModel model, GeospatialModel geometryByURI) throws JsonProcessingException {
        AreaGetDTO dto = dtoWithoutGeometry(model);

        if (geometryByURI.getGeometry() != null) {
            dto.setGeometry(geometryToGeoJson(geometryByURI.getGeometry()));
        }

        return dto;
    }

    /**
     * @param model Area Model to convert
     * @return Corresponding user DTO
     */
    private static AreaGetDTO dtoWithoutGeometry(AreaModel model) {
        AreaGetDTO dto = new AreaGetDTO();

        dto.setUri(model.getUri());
        dto.setName(model.getName());
        dto.setRdfType(model.getType());
        dto.setAuthor(model.getCreator());

        if (model.getDescription() != null) {
            dto.setDescription(model.getDescription());
        }
        return dto;
    }

    /**
     * Convert Geospatial Model into Area DTO
     *
     * @param geospatialModel Geometry Model to convert
     * @return Corresponding user DTO
     */
    public static AreaGetDTO fromModel(GeospatialModel geospatialModel) {
        AreaGetDTO dto = new AreaGetDTO();
        if (geospatialModel != null) {
            try {
                dto.setRdfType(geospatialModel.getRdfType());
                dto.setUri(new URI(SPARQLDeserializers.getExpandedURI(geospatialModel.getUri())));
                dto.setName(geospatialModel.getName());
                dto.setGeometry(geometryToGeoJson(geospatialModel.getGeometry()));
            } catch (JsonProcessingException | URISyntaxException e) {
                e.printStackTrace();
            }
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

    public URI getAuthor() {
        return author;
    }

    public void setAuthor(URI author) {
        this.author = author;
    }
}
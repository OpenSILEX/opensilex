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
import org.opensilex.core.event.api.EventGetDTO;
import org.opensilex.core.event.dal.EventModel;
import org.opensilex.core.geospatial.dal.GeospatialModel;
import org.opensilex.security.user.api.UserGetDTO;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.OffsetDateTime;
import java.util.Objects;

import static org.opensilex.core.geospatial.dal.GeospatialDAO.geometryToGeoJson;

/**
 * DTO representing JSON for the search area or obtain them by uri
 *
 * @author Jean Philippe VERT
 */
@JsonPropertyOrder({"uri", "rdf_type", "is_structural_area","name", "description", "publisher", "geometry", "event", "publication_date", "last_updated_date"})
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
     * Area type ( true = structural | false = temporal)
     */
    @JsonProperty("is_structural_area")
    protected Boolean isStructuralArea;
    /**
     * geometry of the Area
     */
    protected GeoJsonObject geometry;

    /**
     * description
     */
    protected String description;

    /**
     * publisher
     */
    protected UserGetDTO publisher;

    /**
     * event of the Area
     */
    protected EventGetDTO event;

    @JsonProperty("publication_date")
    protected OffsetDateTime publicationDate;

    @JsonProperty("last_updated_date")
    protected OffsetDateTime lastUpdatedDate;

    /**
     * Convert Area Model into Area DTO
     *
     * @param model         Area Model to convert
     * @param geometryByURI Geometry Model to convert
     * @param eventByURI    Event Model to convert
     * @return Corresponding user DTO
     */
    public static AreaGetDTO fromModel(AreaModel model, GeospatialModel geometryByURI, EventModel eventByURI) throws JsonProcessingException {
        AreaGetDTO dto = dtoWithoutGeometry(model);

        if (geometryByURI.getGeometry() != null) {
            dto.setGeometry(geometryToGeoJson(geometryByURI.getGeometry()));
        }

        if (eventByURI != null) {
            dto.setEvent(EventGetDTO.getDTOFromModel(eventByURI));
        }

        return dto;
    }

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

        if (model.getDescription() != null) {
            dto.setDescription(model.getDescription());
        }
        if (Objects.nonNull(model.getPublicationDate())) {
            dto.setPublicationDate(model.getPublicationDate());
        }
        if (Objects.nonNull(model.getLastUpdateDate())) {
            dto.setLastUpdatedDate(model.getLastUpdateDate());
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

    public Boolean getIsStructuralArea() {
        return isStructuralArea;
    }

    public void setIsStructuralArea(Boolean structuralArea) {
        isStructuralArea = structuralArea;
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

    public UserGetDTO getPublisher() {
        return publisher;
    }

    public void setPublisher(UserGetDTO publisher) {
        this.publisher = publisher;
    }

    public EventGetDTO getEvent() {
        return event;
    }

    public void setEvent(EventGetDTO event) {
        this.event = event;
    }

    public OffsetDateTime getPublicationDate() {
        return publicationDate;
    }

    public void setPublicationDate(OffsetDateTime publicationDate) {
        this.publicationDate = publicationDate;
    }

    public OffsetDateTime getLastUpdatedDate() {
        return lastUpdatedDate;
    }

    public void setLastUpdatedDate(OffsetDateTime lastUpdatedDate) {
        this.lastUpdatedDate = lastUpdatedDate;
    }
}
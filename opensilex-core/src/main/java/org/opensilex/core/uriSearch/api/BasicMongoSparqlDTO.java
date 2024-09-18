/*
 * ******************************************************************************
 *                                     BasicMongoSparqlDTO.java
 *  OpenSILEX
 *  Copyright © INRAE 2024
 *  Creation date:  26 august, 2024
 *  Contact: maximilian.hart@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 * ******************************************************************************
 */
package org.opensilex.core.uriSearch.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.opensilex.nosql.mongodb.MongoModel;
import org.opensilex.server.rest.validation.DateFormat;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * This dto is essentially the same as a NamedResourceDTO, or a dto directly translating a MongoModel's basic parameters.
 * Dates are in String format as MongoModels use Instant whereas SPARQL uses OffsetDateTime
 * I couldn't find anything to fill this role already TODO Make some kind of super model or super dto in a more suited location?
 */
public class BasicMongoSparqlDTO {
    private URI uri;

    private String name;

    @JsonProperty("rdf_type")
    private URI type;

    @JsonProperty("rdf_type_name")
    private String typeLabel;

    @JsonProperty("publisher")
    private URI publisher;

    @JsonProperty("publication_date")
    private String publicationDate;

    @JsonProperty("last_updated_date")
    private String lastUpdatedDate;

    private final static DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(DateFormat.YMDTHMSMSX.toString());

    public static BasicMongoSparqlDTO fromSparqlNamedResourceModel(SPARQLNamedResourceModel model){
        BasicMongoSparqlDTO dto = new BasicMongoSparqlDTO();
        dto.setUri(model.getUri());
        dto.setType(model.getType());
        dto.setTypeLabel(model.getTypeLabel().getDefaultValue());
        dto.setName(model.getName());
        dto.setPublisher(model.getPublisher());
        if(model.getPublicationDate() != null){
            dto.setPublicationDate(model.getPublicationDate().format(dateTimeFormatter));
        }
        if(model.getLastUpdateDate() != null){
            dto.setLastUpdatedDate(model.getLastUpdateDate().format(dateTimeFormatter));
        }
        return dto;
    }

    /**
     *
     * @param model
     * @return a BasicMongoSparqlDTO with all fields completed except name and typename
     */
    public static BasicMongoSparqlDTO fromMongoModel(MongoModel model){
        BasicMongoSparqlDTO dto = new BasicMongoSparqlDTO();
        dto.setUri(model.getUri());
        dto.setType(model.getRdfType());
        dto.setPublisher(model.getPublisher());
        if(model.getPublicationDate() != null){
            dto.setPublicationDate(model.getPublicationDate().toString());
        }
        if(model.getLastUpdateDate() != null){
            dto.setLastUpdatedDate(model.getLastUpdateDate().toString());
        }
        return dto;
    }

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public URI getType() {
        return type;
    }

    public void setType(URI type) {
        this.type = type;
    }

    public String getTypeLabel() {
        return typeLabel;
    }

    public BasicMongoSparqlDTO setTypeLabel(String typeLabel) {
        this.typeLabel = typeLabel;
        return this;
    }

    public URI getPublisher() {
        return publisher;
    }

    public void setPublisher(URI publisher) {
        this.publisher = publisher;
    }

    public String getPublicationDate() {
        return publicationDate;
    }

    public void setPublicationDate(String publicationDate) {
        this.publicationDate = publicationDate;
    }

    public String getLastUpdatedDate() {
        return lastUpdatedDate;
    }

    public void setLastUpdatedDate(String lastUpdatedDate) {
        this.lastUpdatedDate = lastUpdatedDate;
    }

    public String getName() {
        return name;
    }

    public BasicMongoSparqlDTO setName(String name) {
        this.name = name;
        return this;
    }
}

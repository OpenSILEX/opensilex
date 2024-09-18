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
import org.opensilex.sparql.model.SPARQLNamedResourceModel;

import java.net.URI;
import java.time.OffsetDateTime;

/**
 * This dto is essentially the same as a NamedResourceDTO, or a dto directly translating a MongoModel's basic parameters.
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
    private OffsetDateTime publicationDate;

    @JsonProperty("last_updated_date")
    private OffsetDateTime lastUpdatedDate;

    public static BasicMongoSparqlDTO fromSparqlNamedResourceModel(SPARQLNamedResourceModel model){
        BasicMongoSparqlDTO dto = new BasicMongoSparqlDTO();
        dto.setUri(model.getUri());
        dto.setType(model.getType());
        dto.setTypeLabel(model.getTypeLabel().getDefaultValue());
        dto.setName(model.getName());
        dto.setPublisher(model.getPublisher());
        dto.setPublicationDate(model.getPublicationDate());
        dto.setLastUpdatedDate(model.getLastUpdateDate());
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

    public void setTypeLabel(String typeLabel) {
        this.typeLabel = typeLabel;
    }

    public URI getPublisher() {
        return publisher;
    }

    public void setPublisher(URI publisher) {
        this.publisher = publisher;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

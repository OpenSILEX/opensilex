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

import java.net.URI;
import java.time.OffsetDateTime;

/**
 * This dto is essentially the same as a NamedResourceDTO, or a dto directly translating a MongoModel's basic parameters.
 * I couldn't find anything to fill this role already TODO Make some kind of super model or super dto in a more suited location?
 */
public class BasicMongoSparqlDTO {
    protected URI uri;

    @JsonProperty("rdf_type")
    protected URI type;

    @JsonProperty("rdf_type_name")
    protected String typeLabel;

    @JsonProperty("publication_date")
    private OffsetDateTime publicationDate;

    @JsonProperty("last_updated_date")
    private OffsetDateTime lastUpdatedDate;
}

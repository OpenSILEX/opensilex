//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2021
// Contact: hamza.ikiou@inrae.fr, arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.variablesGroup.api;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModelProperty;

import java.net.URI;
import java.time.OffsetDateTime;

import org.opensilex.security.user.api.UserGetDTO;
import org.opensilex.server.rest.validation.Required;

/**
 * @author Hamza Ikiou
 */
public abstract class VariablesGroupDTO {
    
    @JsonProperty("uri")
    protected URI uri;

    @JsonProperty("publisher")
    protected UserGetDTO publisher;

    @JsonProperty("publication_date")
    protected OffsetDateTime publicationDate;

    @JsonProperty("last_updated_date")
    protected OffsetDateTime lastUpdatedDate;

    @JsonProperty("name")
    protected String name;
    
    @JsonProperty("description")
    protected String description;
    
    
    public URI getUri() {
        return uri;
    }

    public VariablesGroupDTO setUri(URI uri) {
        this.uri = uri;
        return this;
    }
    
    @Required
    @ApiModelProperty(example = "group of plants", required=true)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UserGetDTO getPublisher() {
        return publisher;
    }

    public void setPublisher(UserGetDTO publisher) {
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

    @ApiModelProperty(example = "Group where we can find all plant related variables")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
    
}

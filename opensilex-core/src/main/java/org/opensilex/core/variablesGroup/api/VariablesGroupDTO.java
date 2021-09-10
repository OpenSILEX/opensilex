//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2021
// Contact: hamza.ikiou@inrae.fr, arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.variablesGroup.api;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModelProperty;

import java.net.URI;

import org.opensilex.server.rest.validation.Required;

/**
 * @author Hamza Ikiou
 */
public abstract class VariablesGroupDTO {
    
    @JsonProperty("uri")
    protected URI uri;

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
    
    @ApiModelProperty(example = "Group where we can find all plant related variables")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
    
}

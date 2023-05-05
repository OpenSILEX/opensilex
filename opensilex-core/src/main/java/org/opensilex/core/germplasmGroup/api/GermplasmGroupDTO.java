//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2021
// Contact: maximilian.hart@inrae.fr, arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.germplasmGroup.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.server.rest.validation.Required;

import java.net.URI;

/**
 * @author Maximilian HART
 */
public abstract class GermplasmGroupDTO {
    
    @JsonProperty("uri")
    protected URI uri;

    @JsonProperty("name")
    protected String name;

    @JsonProperty("description")
    protected String description;

    public URI getUri() {
        return uri;
    }

    public GermplasmGroupDTO setUri(URI uri) {
        this.uri = uri;
        return this;
    }

    @ApiModelProperty(example = "group of genomes")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    @ApiModelProperty(example = "Group where we can find all plant related genomes")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
    
}

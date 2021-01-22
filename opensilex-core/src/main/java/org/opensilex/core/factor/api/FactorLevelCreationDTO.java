/*
 * ******************************************************************************
 *                                     FactorLevelCreationDTO.java
 *  OpenSILEX
 *  Copyright © INRAE 2020
 *  Creation date:  11 March, 2020
 *  Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 * ******************************************************************************
 */
package org.opensilex.core.factor.api;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModelProperty;
import java.net.URI;
import org.opensilex.core.factor.dal.FactorLevelModel;
import org.opensilex.server.rest.validation.Required;
import org.opensilex.server.rest.validation.ValidURI;

/**
 * 
 * @author Arnaud Charleroy
 */
public class FactorLevelCreationDTO {
    @JsonPropertyOrder({"uri", "name", "description"})

    @ValidURI
    @ApiModelProperty(example = "http://opensilex.dev/set/factors#irrigation.ww")
    private URI uri;

    @Required
    @ApiModelProperty(example = "WW")
    private String name;
    
    @ApiModelProperty(example = "Well watered constraint")
    private String description;

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public FactorLevelModel newModel() {
        FactorLevelModel model = new FactorLevelModel();
        model.setUri(getUri());
        model.setName(getName());
        model.setDescription(getDescription());
        return model;
    }
}

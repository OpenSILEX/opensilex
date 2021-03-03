/*
 * ******************************************************************************
 *                                     FactorLevelGetDTO.java
 *  OpenSILEX
 *  Copyright © INRAE 2020
 *  Creation date:  11 March, 2020
 *  Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 * ******************************************************************************
 */
package org.opensilex.core.experiment.factor.api;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModelProperty;
import java.net.URI;
import org.opensilex.core.experiment.factor.dal.FactorLevelModel;

/**
 * 
 * @author Arnaud Charleroy
 */
public class FactorLevelGetDTO {
    @JsonPropertyOrder({"uri", "name", "description"})
       
    protected URI uri;

    protected String name;

    protected String description;
    
    @ApiModelProperty(example = "http://opensilex.dev/set/factors#irrigation.ww")
    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    @ApiModelProperty(example = "WW")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    @ApiModelProperty(example = "Well watered")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
    
    public static FactorLevelGetDTO fromModel(FactorLevelModel model) {
        FactorLevelGetDTO dto = new FactorLevelGetDTO();
        dto.setUri(model.getUri());
        dto.setName(model.getName());
        dto.setDescription(model.getDescription());
        return dto;
    }
}

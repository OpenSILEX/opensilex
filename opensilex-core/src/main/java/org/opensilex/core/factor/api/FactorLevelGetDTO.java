/*
 * ******************************************************************************
 *                                     FactorLevelGetDTO.java
 *  OpenSILEX
 *  Copyright © INRAE 2020
 *  Creation date:  11 March, 2020
 *  Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 * ******************************************************************************
 */
package org.opensilex.core.factor.api;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.net.URI;
import org.opensilex.core.factor.dal.FactorLevelModel;

/**
 * 
 * @author Arnaud Charleroy
 */
public class FactorLevelGetDTO {
    @JsonPropertyOrder({"uri", "name", "description"})
       
    private URI uri;

    private String name;

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
    
    public static FactorLevelGetDTO fromModel(FactorLevelModel model) {
        FactorLevelGetDTO dto = new FactorLevelGetDTO();

        dto.setUri(model.getUri());
        dto.setName(model.getName());
        dto.setDescription(model.getDescription());

        return dto;
    }
}

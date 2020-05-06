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

import java.net.URI;
import org.opensilex.core.factor.dal.FactorLevelModel;

/**
 * 
 * @author Arnaud Charleroy
 */
public class FactorLevelGetDTO {
    
    private URI uri;

    private String name;

    private String comment;
    
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
   
    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
    
    public static FactorLevelGetDTO fromModel(FactorLevelModel model) {
        FactorLevelGetDTO dto = new FactorLevelGetDTO();

        dto.setUri(model.getUri());
        dto.setName(model.getName());
        dto.setComment(model.getComment());

        return dto;
    }
}

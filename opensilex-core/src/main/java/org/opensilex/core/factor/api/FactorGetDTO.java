/*
 * ******************************************************************************
 *                                     FactorGetDTO.java
 *  OpenSILEX
 *  Copyright © INRA 2019
 *  Creation date:  17 December, 2019
 *  Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 * ******************************************************************************
 */
package org.opensilex.core.factor.api;

import java.net.URI;
import org.opensilex.core.factor.dal.FactorModel;

/**
 *
 * @author Arnaud Charleroy
 */
public class FactorGetDTO {
    
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
    
    public static FactorGetDTO fromModel(FactorModel model) {
        FactorGetDTO dto = new FactorGetDTO();
        dto.setUri(model.getUri());        
        if (model.getName() == null) {
            dto.setName(null);
        } else {
            dto.setName(model.getName().getDefaultValue());
        }
        dto.setComment(model.getComment());
        return dto;
    }
}

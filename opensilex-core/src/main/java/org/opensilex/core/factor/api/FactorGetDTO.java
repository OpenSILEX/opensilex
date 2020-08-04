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
    
    private String category;
    
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
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public static FactorGetDTO fromModel(FactorModel model) {
        FactorGetDTO dto = new FactorGetDTO();
        dto.setUri(model.getUri());        
        dto.setName(model.getName());
        dto.setComment(model.getComment());
        dto.setCategory(model.getCategory());
        return dto;
    }
}

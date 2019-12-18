/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.factor.api;

import java.net.URI;
import org.opensilex.core.factor.dal.FactorModel;

/**
 * 
 * @author charlero
 */
public class FactorGetDTO {
    
    private URI uri;

    private String alias;

    private String comment;

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
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
        dto.setAlias(model.getAlias());
        dto.setComment(model.getComment());
        
        return dto;
    }
}

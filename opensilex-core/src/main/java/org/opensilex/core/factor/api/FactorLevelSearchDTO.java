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

import java.net.URI;
import org.opensilex.core.factor.dal.FactorLevelModel;
import org.opensilex.server.rest.validation.Required;

/**
 * 
 * @author Arnaud Charleroy
 */
public class FactorLevelSearchDTO {

    private URI uri;

    @Required
    private String alias;

    private URI hasFactor;
    
    private String comment;

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public String getAlias() {
        if(alias != null && alias.trim().isEmpty()){
            return null;
        } 
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

    public URI getHasFactor() {
        if(hasFactor != null && hasFactor.toString().trim().isEmpty()){
            return null;
        } 
        return hasFactor;
    }

    public void setHasFactor(URI hasFactor) {
        this.hasFactor = hasFactor;
    }
    
    

    public FactorLevelModel newModel() {
        FactorLevelModel model = new FactorLevelModel();
        model.setUri(getUri());
        model.setAlias(getAlias());
        model.setComment(getComment());
        model.setHasFactor(getHasFactor());
        return model;
    }

}

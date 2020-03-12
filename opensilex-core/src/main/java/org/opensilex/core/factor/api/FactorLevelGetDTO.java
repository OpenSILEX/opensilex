/*
 * ******************************************************************************
 *                                     FactorLevelGetDTO.java
 *  OpenSILEX
 *  Copyright © INRAE 2020
 *  Creation date:  11 March, 2020
 *  Contact: arnaud.charleroy@inra.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 * ******************************************************************************
 */
package org.opensilex.core.factor.api;

import java.net.URI;
import org.opensilex.core.factor.dal.FactorLevelModel;
import org.opensilex.core.factor.dal.FactorModel;

/**
 * 
 * @author Arnaud Charleroy
 */
public class FactorLevelGetDTO {
    
    private URI uri;

    private String alias;

    private String comment;
    
    private URI hasFactor;

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

    public URI getHasFactor() {
        return hasFactor;
    }

    public void setHasFactor(URI hasFactor) {
        this.hasFactor = hasFactor;
    }
    
    public static FactorLevelGetDTO fromModel(FactorLevelModel model) {
        FactorLevelGetDTO dto = new FactorLevelGetDTO();

        dto.setUri(model.getUri());
        dto.setAlias(model.getAlias());
        dto.setComment(model.getComment());
        dto.setHasFactor(model.getHasFactor());

        return dto;
    }
}

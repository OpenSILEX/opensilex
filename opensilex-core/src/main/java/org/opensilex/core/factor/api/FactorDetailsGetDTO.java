/*
 * ******************************************************************************
 *                                     FactorDetailsGetDTO.java
 *  OpenSILEX
 *  Copyright © INRAE 2020
 *  Creation date:  30 March, 2020
 *  Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 * ******************************************************************************
 */
package org.opensilex.core.factor.api;

import java.net.URI;
import org.opensilex.core.factor.dal.FactorModel;
import org.opensilex.core.ontology.SKOSReferencesDTO;

/**
 * 
 * @author Arnaud Charleroy
 */
public class FactorDetailsGetDTO extends SKOSReferencesDTO{
    
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
 
    public static FactorDetailsGetDTO fromModel(FactorModel model) {
        FactorDetailsGetDTO dto = new FactorDetailsGetDTO();

        dto.setUri(model.getUri());
        dto.setAlias(model.getAlias());
        dto.setComment(model.getComment());
        dto.setSkosReferencesFromModel(model);

        return dto;
    }
}

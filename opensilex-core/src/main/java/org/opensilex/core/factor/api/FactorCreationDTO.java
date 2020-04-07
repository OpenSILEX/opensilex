/*
 * ******************************************************************************
 *                                     FactorCreationDTO.java
 *  OpenSILEX
 *  Copyright © INRA 2019
 *  Creation date:  17 December, 2019
 *  Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 * ******************************************************************************
 */
package org.opensilex.core.factor.api;

import java.net.URI;
import org.opensilex.core.factor.dal.FactorModel;
import org.opensilex.core.ontology.SKOSReferencesDTO;
import org.opensilex.server.rest.validation.Required;

/**
 * 
 * @author Arnaud Charleroy
 */
public class FactorCreationDTO extends SKOSReferencesDTO {

    private URI uri;

    @Required
    private String alias;

    @Required
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

    public FactorModel newModel() {
        FactorModel model = new FactorModel();
        model.setUri(getUri());
        model.setAlias(getAlias());
        model.setComment(getComment());
        model.setSkosReferencesNewModel(this);
        return model;
    }
}

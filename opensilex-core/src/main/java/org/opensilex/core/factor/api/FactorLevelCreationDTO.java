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
import org.opensilex.server.rest.validation.ValidURI;

/**
 * 
 * @author Arnaud Charleroy
 */
public class FactorLevelCreationDTO {

    @ValidURI
    private URI uri;

    @Required
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

    public FactorLevelModel newModel() {
        FactorLevelModel model = new FactorLevelModel();
        model.setUri(getUri());
        model.setName(getName());
        model.setComment(getComment());
        return model;
    }
}

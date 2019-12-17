/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.factor.api;

import java.net.URI;
import org.opensilex.core.factor.dal.FactorModel;
import org.opensilex.server.validation.Required;

/**
 * 
 * @author charlero
 */
public class FactorCreationDTO {

    private URI uri;

    @Required
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

    public FactorModel newModel() {
        FactorModel model = new FactorModel();
        model.setUri(getUri());
        model.setAlias(getAlias());
        model.setComment(getComment());

        return model;
    }

}

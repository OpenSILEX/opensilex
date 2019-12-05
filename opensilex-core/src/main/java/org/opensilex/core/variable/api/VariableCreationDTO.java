/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.variable.api;

import java.net.URI;
import org.opensilex.core.variable.dal.VariableModel;

/**
 *
 * @author vidalmor
 */
public class VariableCreationDTO extends VariableUpdateDTO {

    protected URI uri;

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public VariableModel defineModel(VariableModel model) {
        model = super.defineModel(model);
        model.setUri(getUri());

        return model;
    }
}

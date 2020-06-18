/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.variable.api.unit;

import java.net.URI;
import org.opensilex.core.variable.dal.unit.UnitModel;

/**
 *
 * @author vidalmor
 */
public class UnitCreationDTO extends UnitUpdateDTO {

    protected URI uri;

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public UnitModel defineModel(UnitModel model) {
        model = super.defineModel(model);
        model.setUri(getUri());

        return model;
    }
}

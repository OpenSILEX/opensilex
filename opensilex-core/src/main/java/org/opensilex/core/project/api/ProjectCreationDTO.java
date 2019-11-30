/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.project.api;

import java.net.*;
import org.opensilex.core.project.dal.*;

/**
 *
 * @author vidalmor
 */
public class ProjectCreationDTO extends ProjectUpdateDTO {

    protected URI uri;

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }
    
    public ProjectModel updateModel(ProjectModel model) {
        model = super.updateModel(model);
        if (getUri() != null) {
            model.setUri(getUri());
        }
        
        return model;
    }
}

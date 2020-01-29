/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.project.api;

import java.net.URI;
import org.opensilex.core.project.dal.ProjectModel;


/**
 *
 * @author vidalmor
 */
public class ProjectGetDTO {

    private URI uri;

    private String label;

    private String comment;

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public static ProjectGetDTO fromModel(ProjectModel model) {
        ProjectGetDTO dto = new ProjectGetDTO();

        dto.setUri(model.getUri());
        dto.setLabel(model.getName());
        //dto.setComment(model.getComment());
        
        return dto;
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.ontology.api;

import java.net.URI;
import org.opensilex.core.ontology.dal.ClassModel;

/**
 *
 * @author vmigot
 */
public class RDFClassDTO {

    protected URI uri;

    protected String label;

    protected String comment;

    protected URI parent;

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

    public URI getParent() {
        return parent;
    }

    public void setParent(URI parent) {
        this.parent = parent;
    }

    public static RDFClassDTO fromModel(RDFClassDTO dto, ClassModel model) {
        dto.setUri(model.getUri());
        dto.setLabel(model.getName());
        if (model.getComment() != null) {
            dto.setComment(model.getComment().getDefaultValue());
        }

        if (model.getParent() != null) {
            dto.setParent(model.getParent().getUri());
        }

        return dto;
    }
}

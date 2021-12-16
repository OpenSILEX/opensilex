/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.ontology.api;

import org.opensilex.sparql.ontology.dal.ClassModel;

import java.net.URI;

/**
 *
 * @author vmigot
 */
public class RDFTypeDTO {

    protected URI uri;

    protected String name;

    protected String comment;

    protected URI parent;

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

    public URI getParent() {
        return parent;
    }

    public void setParent(URI parent) {
        this.parent = parent;
    }

    public static RDFTypeDTO fromModel(RDFTypeDTO dto, ClassModel model) {
        dto.setUri(model.getUri());

        if (model.getLabel() != null) {
            dto.setName(model.getLabel().getDefaultValue());
        }
        if (model.getComment() != null) {
            dto.setComment(model.getComment().getDefaultValue());
        }

        if (model.getParent() != null) {
            dto.setParent(model.getParent().getUri());
        }

        return dto;
    }
}

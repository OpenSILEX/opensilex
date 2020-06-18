/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.variable.api.method;

import java.net.URI;

import org.opensilex.core.ontology.SKOSReferencesDTO;
import org.opensilex.core.variable.dal.method.MethodModel;


/**
 *
 * @author vidalmor
 */
public class MethodGetDTO extends SKOSReferencesDTO {

    private URI uri;

    private String label;

    private String comment;

    private URI type;

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

    public URI getType() { return type; }

    public void setType(URI type) { this.type = type; }

    public static MethodGetDTO fromModel(MethodModel model) {
        MethodGetDTO dto = new MethodGetDTO();

        dto.uri = model.getUri();
        dto.label = model.getName();
        dto.comment = model.getComment();
        dto.type = model.getType();

        dto.setSkosReferencesFromModel(model);
        return dto;
    }
}

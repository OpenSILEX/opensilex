/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.variable.api;

import java.net.*;
import org.opensilex.core.variable.dal.*;

/**
 *
 * @author vidalmor
 */
public class MethodGetDTO {

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

    public static MethodGetDTO fromModel(MethodModel model) {
        MethodGetDTO dto = new MethodGetDTO();

        dto.setUri(model.getUri());
        dto.setLabel(model.getName());
        dto.setComment(model.getComment());

        return dto;
    }
}

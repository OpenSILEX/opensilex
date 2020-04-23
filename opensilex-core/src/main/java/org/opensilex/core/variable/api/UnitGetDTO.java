/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.variable.api;

import java.net.URI;
import org.opensilex.core.variable.dal.UnitModel;


/**
 *
 * @author vidalmor
 */
public class UnitGetDTO {

    private URI uri;

    private String label;

    private String comment;

    private String dimension;

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

    public String getDimension() {
        return dimension;
    }

    public void setDimension(String dimension) {
        this.dimension = dimension;
    }

    public static UnitGetDTO fromModel(UnitModel model) {
        UnitGetDTO dto = new UnitGetDTO();

        dto.setUri(model.getUri());
        dto.setLabel(model.getLabel());
        dto.setComment(model.getComment());
        dto.setDimension(model.getDimension());

        return dto;
    }
}

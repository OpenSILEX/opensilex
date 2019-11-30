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
public class VariableGetDTO {

    private URI uri;

    private String label;

    private String comment;

    private EntityGetDTO entity;

    private QualityGetDTO quality;

    private MethodGetDTO method;

    private UnitGetDTO unit;

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

    public EntityGetDTO getEntity() {
        return entity;
    }

    public void setEntity(EntityGetDTO entity) {
        this.entity = entity;
    }

    public QualityGetDTO getQuality() {
        return quality;
    }

    public void setQuality(QualityGetDTO quality) {
        this.quality = quality;
    }

    public MethodGetDTO getMethod() {
        return method;
    }

    public void setMethod(MethodGetDTO method) {
        this.method = method;
    }

    public UnitGetDTO getUnit() {
        return unit;
    }

    public void setUnit(UnitGetDTO unit) {
        this.unit = unit;
    }

    public static VariableGetDTO fromModel(VariableModel model) {
        VariableGetDTO dto = new VariableGetDTO();

        dto.setLabel(model.getName());
        dto.setComment(model.getComment());

        dto.setEntity(EntityGetDTO.fromModel(model.getEntity()));
        dto.setQuality(QualityGetDTO.fromModel(model.getQuality()));
        dto.setMethod(MethodGetDTO.fromModel(model.getMethod()));
        dto.setUnit(UnitGetDTO.fromModel(model.getUnit()));

        return dto;
    }
}

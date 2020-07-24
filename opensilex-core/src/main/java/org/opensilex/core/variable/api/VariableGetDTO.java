/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.variable.api;

import java.net.URI;

import io.swagger.annotations.ApiModelProperty;
import org.opensilex.core.variable.dal.EntityModel;
import org.opensilex.core.variable.dal.MethodModel;
import org.opensilex.core.variable.dal.QualityModel;
import org.opensilex.core.variable.dal.UnitModel;
import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.sparql.response.NamedResourceDTO;


/**
 *
 * @author vidalmor
 */
public class VariableGetDTO {

    private URI uri;

    private String name;

    private NamedResourceDTO<EntityModel> entity;

    private NamedResourceDTO<QualityModel> quality;

    private NamedResourceDTO<MethodModel> method;

    private NamedResourceDTO<UnitModel> unit;


    @ApiModelProperty(example = "http://opensilex.dev/set/variables/Plant_Height")
    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    @ApiModelProperty(example = "Plant_Height")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public NamedResourceDTO<EntityModel> getEntity() { return entity; }

    public void setEntity(NamedResourceDTO<EntityModel> entity) {
        this.entity = entity;
    }

    public NamedResourceDTO<QualityModel> getQuality() {
        return quality;
    }

    public void setQuality(NamedResourceDTO<QualityModel> quality) { this.quality = quality; }

    public NamedResourceDTO<MethodModel> getMethod() { return method; }

    public void setMethod(NamedResourceDTO<MethodModel> method) { this.method = method; }

    public NamedResourceDTO<UnitModel> getUnit() {
        return unit;
    }

    public void setUnit(NamedResourceDTO<UnitModel> unit) {
        this.unit = unit;
    }


    public static VariableGetDTO fromModel(VariableModel model) {

        VariableGetDTO dto = new VariableGetDTO();
        dto.setUri(model.getUri());
        dto.setName(model.getName());

        NamedResourceDTO<EntityModel> entityDto = new NamedResourceDTO<>();
        EntityModel entity = model.getEntity();
        entityDto.setUri(entity.getUri());
        entityDto.setName(entity.getName());
        dto.setEntity(entityDto);

        NamedResourceDTO<QualityModel> qualityDto = new NamedResourceDTO<>();
        QualityModel quality = model.getQuality();
        qualityDto.setUri(quality.getUri());
        qualityDto.setName(quality.getName());
        dto.setQuality(qualityDto);

        NamedResourceDTO<MethodModel> methodDto = new NamedResourceDTO<>();
        MethodModel method = model.getMethod();
        methodDto.setUri(method.getUri());
        methodDto.setName(method.getName());
        dto.setMethod(methodDto);

        NamedResourceDTO<UnitModel> unitDto = new NamedResourceDTO<>();
        UnitModel unit = model.getUnit();
        unitDto.setUri(unit.getUri());
        unitDto.setName(unit.getName());
        dto.setUnit(unitDto);

        return dto;
    }
}


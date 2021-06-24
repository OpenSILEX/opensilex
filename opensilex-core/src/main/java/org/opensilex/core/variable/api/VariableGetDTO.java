/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.variable.api;

import java.net.URI;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.core.variable.api.entity.EntityGetDTO;
import org.opensilex.core.variable.api.method.MethodGetDTO;
import org.opensilex.core.variable.api.characteristic.CharacteristicGetDTO;
import org.opensilex.core.variable.api.unit.UnitGetDTO;
import org.opensilex.core.variable.dal.EntityModel;
import org.opensilex.core.variable.dal.MethodModel;
import org.opensilex.core.variable.dal.CharacteristicModel;
import org.opensilex.core.variable.dal.UnitModel;
import org.opensilex.core.variable.dal.VariableModel;

/**
 *
 * @author vidalmor
 */
@JsonPropertyOrder({
        "uri", "name", "entity", "characteristic", "method", "unit"
})
public class VariableGetDTO {

    @JsonProperty("uri")
    private URI uri;

    @JsonProperty("name")
    private String name;

    @JsonProperty("entity")
    private EntityGetDTO entity;

    @JsonProperty("characteristic")
    private CharacteristicGetDTO characteristic;

    @JsonProperty("method")
    private MethodGetDTO method;

    @JsonProperty("unit")
    private UnitGetDTO unit;
    
     @JsonProperty("rdf_type")
    protected URI type;


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

    public EntityGetDTO getEntity() { return entity; }

    public void setEntity(EntityGetDTO entity) {
        this.entity = entity;
    }

    public CharacteristicGetDTO getCharacteristic() {
        return characteristic;
    }

    public void setCharacteristic(CharacteristicGetDTO characteristic) { this.characteristic = characteristic; }

    public MethodGetDTO getMethod() { return method; }

    public void setMethod(MethodGetDTO method) { this.method = method; }

    public UnitGetDTO getUnit() {
        return unit;
    }

    public void setUnit(UnitGetDTO unit) {
        this.unit = unit;
    }
     public VariableGetDTO setType(URI rdfType) {
        this.type = rdfType;
        return this;
    }
     
     public URI getType() {
        return type;
    }



    public static VariableGetDTO fromModel(VariableModel model) {

        VariableGetDTO dto = new VariableGetDTO();
        dto.setUri(model.getUri());
        dto.setType(model.getType());
        dto.setName(model.getName());

        EntityModel entity = model.getEntity();
        dto.setEntity(new EntityGetDTO(entity));

        CharacteristicModel characteristic = model.getCharacteristic();
        dto.setCharacteristic(new CharacteristicGetDTO(characteristic));

        MethodModel method = model.getMethod();
        if(method != null){
            dto.setMethod(new MethodGetDTO(method));
        }

        UnitModel unit = model.getUnit();
        dto.setUnit(new UnitGetDTO(unit));

        return dto;
    }
}


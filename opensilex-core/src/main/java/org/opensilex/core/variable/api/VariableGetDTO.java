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
import org.opensilex.core.variable.dal.*;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;
import org.opensilex.sparql.response.NamedResourceDTO;

/**
 *
 * @author vidalmor
 */
@JsonPropertyOrder({
        "uri", "name", "entity", "entity_of_interest", "characteristic", "method", "unit"
})
public class VariableGetDTO {

    @JsonProperty("uri")
    private URI uri;

    @JsonProperty("name")
    private String name;

    @JsonProperty("entity")
    private EntityGetDTO entity;
    
    @JsonProperty("entity_of_interest")
    private NamedResourceDTO entityOfInterest;
    
    @JsonProperty("characteristic")
    private CharacteristicGetDTO characteristic;

    @JsonProperty("method")
    private MethodGetDTO method;

    @JsonProperty("unit")
    private UnitGetDTO unit;

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
    
    public NamedResourceDTO getEntityOfInterest(){
        return entityOfInterest;
    } 
    
    public void setEntityOfInterest(NamedResourceDTO entityOfInterest){
        this.entityOfInterest = entityOfInterest;
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


    public static VariableGetDTO fromModel(VariableModel model) {

        VariableGetDTO dto = new VariableGetDTO();
        dto.setUri(model.getUri());
        dto.setName(model.getName());

        EntityModel entity = model.getEntity();
        dto.setEntity(new EntityGetDTO(entity));
        
        InterestEntityModel entityOfInterest = model.getEntityOfInterest();
        if(entityOfInterest != null){
            dto.setEntityOfInterest(NamedResourceDTO.getDTOFromModel(entityOfInterest));
        }
                
        CharacteristicModel characteristic = model.getCharacteristic();
        dto.setCharacteristic(new CharacteristicGetDTO(characteristic));

        MethodModel method = model.getMethod();
        dto.setMethod(new MethodGetDTO(method));
        
        UnitModel unit = model.getUnit();
        dto.setUnit(new UnitGetDTO(unit));

        return dto;
    }
}


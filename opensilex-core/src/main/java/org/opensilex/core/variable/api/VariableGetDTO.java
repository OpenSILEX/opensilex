/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.variable.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.core.sharedResource.SharedResourceInstanceDTO;
import org.opensilex.core.variable.api.characteristic.CharacteristicGetDTO;
import org.opensilex.core.variable.api.entity.EntityGetDTO;
import org.opensilex.core.variable.api.method.MethodGetDTO;
import org.opensilex.core.variable.api.unit.UnitGetDTO;
import org.opensilex.core.variable.dal.*;
import org.opensilex.sparql.response.NamedResourceDTO;

import java.net.URI;
import java.util.List;

/**
 *
 * @author vidalmor
 */
@JsonPropertyOrder({
        "uri", "name", "entity", "entity_of_interest", "characteristic", "method", "unit", "onLocal", "sharedResourceInstance"
})
public class VariableGetDTO {

    @JsonProperty("uri")
    private URI uri;

    @JsonProperty("name")
    private String name;

    @JsonProperty("alternative_name")
    private String alternativeName;

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

    @JsonProperty("onLocal")
    private boolean onLocal;

    @JsonProperty("sharedResourceInstance")
    private SharedResourceInstanceDTO sharedResourceInstance;

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

    public String getAlternativeName() {
        return alternativeName;
    }

    public void setAlternativeName(String alternativeName) {
        this.alternativeName = alternativeName;
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

    public boolean getOnLocal() {
        return onLocal;
    }

    public void setOnLocal(boolean onLocal) {
        this.onLocal = onLocal;
    }

    public SharedResourceInstanceDTO getSharedResourceInstance() {
        return sharedResourceInstance;
    }

    public void setSharedResourceInstance(SharedResourceInstanceDTO sharedResourceInstance) {
        this.sharedResourceInstance = sharedResourceInstance;
    }

    /**
     * Creates a {@link VariableGetDTO} from a {@link VariableModel}. The list of shared resource instances should be
     * provided to set the field {@link VariableGetDTO#sharedResourceInstance} correctly.
     *
     * @param model The source model
     * @param sharedResourceInstanceList The list of shared resource instances, as returned by
     * {@link org.opensilex.core.CoreModule#getSharedResourceInstancesFromConfiguration(String)}.
     * @return The corresponding {@link VariableGetDTO}
     */
    public static VariableGetDTO fromModel(VariableModel model, List<SharedResourceInstanceDTO> sharedResourceInstanceList) {

        VariableGetDTO dto = new VariableGetDTO();
        dto.setUri(model.getUri());
        dto.setName(model.getName());
        dto.setAlternativeName(model.getAlternativeName());

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

        URI sriUri = model.getFromSharedResourceInstance();
        if(sriUri != null){
            if (sharedResourceInstanceList != null) {
                dto.setSharedResourceInstance(sharedResourceInstanceList.stream()
                        .filter(sri -> sri.getUri().equals(sriUri))
                        .findFirst()
                        .orElse(new SharedResourceInstanceDTO().setUri(sriUri)));
            } else {
                dto.setSharedResourceInstance(new SharedResourceInstanceDTO().setUri(sriUri));
            }
        }
        return dto;
    }
}

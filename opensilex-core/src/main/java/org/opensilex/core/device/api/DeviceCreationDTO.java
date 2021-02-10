/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.device.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.Map;
import org.opensilex.core.device.dal.DeviceModel;

/**
 *
 * @author sammy
 */

@JsonPropertyOrder({"uri","type","name","brand",
    "constructor_model","serial_number","person_in_charge","start_up",
    "removal","relations", "description", "metadata"})
public class DeviceCreationDTO extends DeviceDTO {
    
    @JsonProperty("metadata")
    protected Map<String, String> metadata;

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }
    
    public void toModel(DeviceModel model) {
        model.setUri(getUri());
        model.setType(getType());
        model.setName(getName());
        if (getBrand() != null) {
            model.setBrand(getBrand());
        }
        
        if(getConstructorModel() != null){
            model.setModel(getConstructorModel());
        }
        
        if(getSerialNumber() != null){
            model.setSerialNumber(getSerialNumber());
        }
        
        if(getPersonInCharge() != null){
            model.setPersonInCharge(getPersonInCharge());
        }
        
        if(getStartUp() != null){
            model.setStartUp(getStartUp());
        }

        if(getRemoval() != null){
            model.setRemoval(getRemoval());
        }
        
        if(getDescription() != null){
            model.setDescription(getDescription());
        }
        
        if (metadata != null ) {
           model.setAttributes(metadata);
        }
    }
    
    public DeviceModel newModel() {
        DeviceModel instance = newModelInstance();
        toModel(instance);
        return instance;
    }
}

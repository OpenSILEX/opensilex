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

@JsonPropertyOrder({"uri","rdf_type","rdf_type_name","name","brand",
    "constructor_model","serial_number","person_in_charge","start_up",
    "removal","relations", "description", "metadata"})

public class DeviceGetDetailsDTO extends DeviceGetDTO{
    
    @JsonProperty("metadata")
    protected Map<String, String> metadata;

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }
    
    @Override
    public void fromModel(DeviceModel model) {
        super.fromModel(model);
        
        if (model.getMetaDataModel() != null) {
            setMetadata(model.getMetaDataModel().getAttributes());
        }
    }
    
    public static DeviceGetDetailsDTO getDTOFromModel(DeviceModel model) {
        DeviceGetDetailsDTO dto = new DeviceGetDetailsDTO();
        dto.fromModel(model);

        return dto;
    }
}

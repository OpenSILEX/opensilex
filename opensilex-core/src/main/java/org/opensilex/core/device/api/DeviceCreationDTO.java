/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.device.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModelProperty;
import java.net.URI;
import java.util.Map;
import javax.validation.constraints.NotNull;
import org.opensilex.core.device.dal.DeviceModel;
import org.opensilex.server.rest.validation.ValidURI;
import org.opensilex.server.rest.validation.DateFormat;
import org.opensilex.server.rest.validation.Date;

/**
 *
 * @author sammy
 */

@JsonPropertyOrder({"uri","rdf_type","name","brand",
    "constructor_model","serial_number","person_in_charge","start_up",
    "removal","relations", "description", "metadata"})
public class DeviceCreationDTO extends DeviceDTO {
    
    @ValidURI
    @ApiModelProperty(value = "Device URI", example = "http://opensilex.dev/opensilex/set/device/sensingdevice-sensor_01")
    protected URI uri;
    
    @NotNull
    @Override
    public URI getType() {
        return super.getType();
    }
    @NotNull
    @Override
    public String getName() {
        return super.getName();
    }
    
    protected String brand;
    
    @JsonProperty("constructor_model")
    protected String constructorModel;
    
    @JsonProperty("serial_number")
    protected String serialNumber;
    
    @ValidURI
    @JsonProperty("person_in_charge")
    protected URI personInCharge;
    
    @Date(DateFormat.YMD)
    @JsonProperty("start_up")
    protected String startUp;
    
    @Date(DateFormat.YMD)
    protected String removal;
    
    protected String description;
    
    @JsonProperty("metadata")
    protected Map<String, String> metadata;

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }
    
    public void toModel(DeviceModel model) {

        super.toModel(model);
        model.setName(getName());

        model.setBrand(getBrand());
        model.setModel(getConstructorModel());
        model.setSerialNumber(getSerialNumber());

        
        if(getStartUp() != null){
            model.setStartUp(getStartUp());
        }

        if(getRemoval() != null){
            model.setRemoval(getRemoval());
        }
        
        model.setDescription(getDescription());

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

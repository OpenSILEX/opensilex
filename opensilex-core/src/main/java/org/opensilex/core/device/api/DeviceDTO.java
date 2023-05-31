/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.device.api;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URI;
import java.time.LocalDate;

import org.opensilex.core.device.dal.DeviceModel;
import org.opensilex.core.ontology.api.RDFObjectDTO;
import io.swagger.annotations.ApiModelProperty;

/**
 *
 * @author sammy
 */
public class DeviceDTO extends RDFObjectDTO {

    @ApiModelProperty(value = "Device name", example = "Sensor_01", required = true)
    protected String name;
    
    @ApiModelProperty(value = "Device brand", example = "Campbell")
    protected String brand;
    
    @ApiModelProperty(value = "Device model", example = "CS655")
    @JsonProperty("constructor_model")
    protected String constructorModel;
    
    @ApiModelProperty(value = "Device serial number", example = "123456")
    @JsonProperty("serial_number")
    protected String serialNumber;
    
    @ApiModelProperty(value = "Person in charge", example = "http://opensilex.dev/person#Firstname.Lastname")
    @JsonProperty("person_in_charge")
    protected URI personInChargeURI;
    
    @ApiModelProperty(value = "Device date of start-up", example = "2018-12-12")
    @JsonProperty("start_up")
    protected LocalDate startUp;

    @ApiModelProperty(value = "Device date of removal", example = "2020-12-12")
    @JsonProperty("removal")
    protected LocalDate removal;

    @ApiModelProperty(value = "rdfType URI", example = "http://www.opensilex.org/vocabulary/oeso#SensingDevice")
    @JsonProperty("rdf_type")
    protected URI type;
    
    @ApiModelProperty(value = "comment", example = "description")
    @JsonProperty("description")
    protected String description;
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public void setBrand(String brand){
        this.brand = brand;
    }
    
    public void setConstructorModel(String constructorModel){
        this.constructorModel = constructorModel;
    }
    
    public void setSerialNumber(String serialNumber){
        this.serialNumber = serialNumber;
    }
    
    public void setPersonInChargeURI(URI personInChargeURI){
        this.personInChargeURI = personInChargeURI;
    }
    
    public void setStartUp(LocalDate startUp){
        this.startUp = startUp;
    }
    
    public void setRemoval(LocalDate removal){
        this.removal = removal;
    }
    
    public String getBrand(){
        return brand;
    }
    
    public String getConstructorModel(){
        return constructorModel;
    }
    
    public String getSerialNumber(){
        return serialNumber;
    }
    
    public URI getPersonInChargeURI(){
        return personInChargeURI;
    }
    
    public LocalDate getStartUp(){
        return startUp;
    }
    
    public LocalDate getRemoval(){
        return removal;
    }
    
     public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }  
    
    public DeviceModel newModelInstance() {
        return new DeviceModel();
    }
    
}

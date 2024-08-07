/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.device.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.opensilex.core.device.dal.DeviceModel;
import org.opensilex.core.ontology.api.RDFObjectDTO;
import org.opensilex.core.ontology.api.RDFObjectRelationDTO;
import org.opensilex.sparql.model.SPARQLModelRelation;

/**
 *
 * @author sammy
 */
public class DeviceExportDTO extends RDFObjectDTO{
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
    
    @ApiModelProperty(value = "Person in charge", example = "http://opensilex.dev/users#Firstname.Lastname")
    @JsonProperty("person_in_charge")
    protected URI personInCharge;
    
    @ApiModelProperty(value = "Device date of start-up", example = "2018-12-12")
    @JsonProperty("start_up")
    protected String startUp;

    @ApiModelProperty(value = "Device date of removal", example = "2020-12-12")
    @JsonProperty("removal")
    protected String removal;

    @ApiModelProperty(value = "rdfType URI", example = "http://www.opensilex.org/vocabulary/oeso#SensingDevice")
    @JsonProperty("rdf_type")
    protected URI type;
    
    @ApiModelProperty(value = "comment", example = "description")
    @JsonProperty("description")
    protected String description;
    
    @JsonProperty("metadata")
    protected Map<String, String> metadata;

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }
    
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
    
    public void setPersonInCharge(URI personInCharge){
        this.personInCharge = personInCharge;
    }
    
    public void setStartUp(String startUp){
        this.startUp = startUp;
    }
    
    public void setRemoval(String removal){
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
    
    public URI getPersonInCharge(){
        return personInCharge;
    }
    
    public String getStartUp(){
        return startUp;
    }
    
    public String getRemoval(){
        return removal;
    }
    
     public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }  
    
    public void fromModel(DeviceModel model) {
        setUri(model.getUri());
        setType(model.getType());
        setName(model.getName());
        if (model.getBrand() != null) {
            setBrand(model.getBrand());
        }
        
        if(model.getModel() != null){
            setConstructorModel(model.getModel());
        }
        
        if(model.getSerialNumber() != null){
            setSerialNumber(model.getSerialNumber());
        }
        
        if(model.getPersonInCharge() != null){
            setPersonInCharge(model.getPersonInCharge().getUri());
        }
        
        if(model.getStartUp() != null){
            setStartUp(model.getStartUp().toString());
        }
        
        if(model.getRemoval() != null){
            setRemoval(model.getRemoval().toString());
        }
        
        if(model.getDescription() != null){
            setDescription(model.getDescription());
        }
        
        List<RDFObjectRelationDTO> relationsDTO = new ArrayList<>(model.getRelations().size());
        for (SPARQLModelRelation relation : model.getRelations()) {
            relationsDTO.add(RDFObjectRelationDTO.getDTOFromModel(relation));
        }
        setRelations(relationsDTO);
        
        if (model.getMetaDataModel() != null) {
            setMetadata(model.getMetaDataModel().getAttributes());
        }
    }
    
    public DeviceModel newModelInstance() {
        return new DeviceModel();
    }
    
    public static DeviceExportDTO getDTOFromModel(DeviceModel model) {
        DeviceExportDTO dto = new DeviceExportDTO();
        dto.fromModel(model);

        return dto;
    }
}

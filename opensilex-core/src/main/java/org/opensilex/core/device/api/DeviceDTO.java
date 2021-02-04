/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.device.api;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.opensilex.core.device.dal.DeviceModel;
import org.opensilex.core.ontology.api.RDFObjectDTO;
import org.opensilex.core.ontology.api.RDFObjectRelationDTO;
import org.opensilex.sparql.model.SPARQLLabel;
import org.opensilex.sparql.model.SPARQLModelRelation;
import org.opensilex.sparql.response.NamedResourceDTO;

/**
 *
 * @author sammy
 */
@JsonPropertyOrder({"uri","type","rdf_type_name","name","brand",
    "constructor_model","serial_number","person_in_charge","start_up",
    "removal","relations", "description"})

public class DeviceDTO extends RDFObjectDTO {
    
    private String name;
    
    protected String brand;
    
    @JsonProperty("constructor_model")
    protected String constructorModel;
    
    @JsonProperty("serial_number")
    protected String serialNumber;
    
    @JsonProperty("person_in_charge")
    protected URI personInCharge;
    
    @JsonProperty("start_up")
    protected LocalDate startUp;
    
    @JsonProperty("removal")
    protected LocalDate removal;
    
    @JsonProperty("rdf_type")
    protected URI type;
    
    @JsonProperty("rdf_type_name")
    protected String typeLabel;
    
    @JsonProperty("description")
    protected String description;

    public void setTypeLabel(String typeLabel){
        this.typeLabel = typeLabel;
    }
    
    public String getTypeLabel(){
        return typeLabel;
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
    
    public URI getPersonInCharge(){
        return personInCharge;
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

    public void fromModel(DeviceModel model) {
        setUri(model.getUri());
        setType(model.getType());
        setTypeLabel(model.getTypeLabel().getDefaultValue());
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
            setPersonInCharge(model.getPersonInCharge());
        }
        
        if(model.getStartUp() != null){
            setStartUp(model.getStartUp());
        }
        
        if(model.getRemoval() != null){
            setRemoval(model.getRemoval());
        }
        
        if(model.getDescription() != null){
            setDescription(model.getDescription());
        }
        
        List<RDFObjectRelationDTO> relationsDTO = new ArrayList<>(model.getRelations().size());
        for (SPARQLModelRelation relation : model.getRelations()) {
            relationsDTO.add(RDFObjectRelationDTO.getDTOFromModel(relation));
        }
        setRelations(relationsDTO);
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
    }
    
    public DeviceModel newModel() {
        DeviceModel instance = newModelInstance();
        toModel(instance);
        return instance;
    }
    
    public static DeviceDTO getDTOFromModel(DeviceModel model) {
        DeviceDTO dto = new DeviceDTO();
        dto.fromModel(model);

        return dto;
    }
}

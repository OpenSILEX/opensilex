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
import java.util.Map;
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
public class DeviceDTO extends RDFObjectDTO {
    
    protected String name;
    
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
    
}

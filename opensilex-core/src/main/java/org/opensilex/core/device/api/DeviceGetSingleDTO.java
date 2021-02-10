/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.device.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.opensilex.core.device.dal.DeviceModel;
import org.opensilex.core.ontology.api.RDFObjectRelationDTO;
import org.opensilex.sparql.model.SPARQLModelRelation;

/**
 *
 * @author sammy
 */

@JsonPropertyOrder({"uri","type","rdf_type_name","name","brand",
    "constructor_model","serial_number","person_in_charge","start_up",
    "removal","relations", "description", "metadata"})

public class DeviceGetSingleDTO extends DeviceDTO{
    @JsonProperty("rdf_type_name")
    protected String typeLabel;
    
    @JsonProperty("metadata")
    protected Map<String, String> metadata;

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }
    
    public void setTypeLabel(String typeLabel){
        this.typeLabel = typeLabel;
    }
    
    public String getTypeLabel(){
        return typeLabel;
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
        
        if (model.getAttributes() != null) {
            setMetadata(model.getAttributes());
        }
    }
    
    public static DeviceGetSingleDTO getDTOFromModel(DeviceModel model) {
        DeviceGetSingleDTO dto = new DeviceGetSingleDTO();
        dto.fromModel(model);

        return dto;
    }
}

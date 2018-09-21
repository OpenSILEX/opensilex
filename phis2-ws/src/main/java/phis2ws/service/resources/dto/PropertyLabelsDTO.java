//******************************************************************************
//                                       PropertyDTO.java
//
// Author(s): Vincent Migot <vincent.migot@inra.fr>
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: 10 septembre 2018
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  10 septembre 2018
// Subject: Represents the submitted JSON for a property
//******************************************************************************
package phis2ws.service.resources.dto;

import io.swagger.annotations.ApiModelProperty;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import phis2ws.service.documentation.DocumentationAnnotation;
import phis2ws.service.view.model.phis.Property;

public class PropertyLabelsDTO extends PropertyDTO {

    private LinkedList<String> rdfTypeLabels = new LinkedList<>();
    private LinkedList<String> relationLabels = new LinkedList<>();
    private LinkedList<String> valueLabels = new LinkedList<>();

    @Override
    public Property createObjectFromDTO() {
        Property property = super.createObjectFromDTO();
        
        property.setRdfTypeLabel(rdfTypeLabels);
        property.setRelationLabel(relationLabels);
        property.setValueLabel(valueLabels);
        
        return property;
    }
    
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_SPECIES_RDF_TYPE)
    public LinkedList<String> getRdfTypeLabels() {
        return rdfTypeLabels;
    }

    public void setRdfTypeLabels(LinkedList<String> rdfTypeLabels) {
        this.rdfTypeLabels = rdfTypeLabels;
    }
    
    public void addFirstRdfTypeLabel(String label) {
        rdfTypeLabels.addFirst(label);
    }
            
    public void addLastRdfTypeLabel(String label) {
        rdfTypeLabels.addLast(label);
    }
            
    public void addRdfTypeLabels(Collection<String> labels) {
        rdfTypeLabels.addAll(labels);
    }
    
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_SPECIES_FROM_SPECIES)
    public LinkedList<String> getRelationLabels() {
        return relationLabels;
    }

    public void setRelationLabels(LinkedList<String> relationLabels) {
        this.relationLabels = relationLabels;
    }
    
    public void addFirstRelationLabel(String label) {
        relationLabels.addFirst(label);
    }
    
    public void addLastRelationLabel(String label) {
        relationLabels.addLast(label);
    }
        
    public void addRelationLabels(Collection<String> labels) {
        relationLabels.addAll(labels);
    }
        
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_SPECIES_URI)
    public LinkedList<String> getValueLabels() {
        return valueLabels;
    }
    
    public void setValueLabel(LinkedList<String> valueLabels) {
        this.valueLabels = valueLabels;
    }
    
    public void addFirstValueLabel(String label) {
        valueLabels.addFirst(label);
    }
    
    public void addLastValueLabel(String label) {
        valueLabels.addLast(label);
    }
    
    public void addValueLabels(Collection<String> labels) {
        valueLabels.addAll(labels);
    }
}

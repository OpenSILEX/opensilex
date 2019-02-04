//******************************************************************************
//                                       PropertyLabelsDTO.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 20 September, 2018
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.resources.dto.rdfResourceDefinition;

import io.swagger.annotations.ApiModelProperty;
import java.util.Collection;
import java.util.LinkedList;
import phis2ws.service.documentation.DocumentationAnnotation;
import phis2ws.service.view.model.phis.Property;

/**
 * Represent the submitted JSON of a property with associated labels
 * @author Vincent Migot <vincent.migot@inra.fr>
 */
public class PropertyLabelsDTO extends PropertyDTO {

    // Labels list for the value "rdfType" of the property
    private LinkedList<String> rdfTypeLabels = new LinkedList<>();
    // Labels list for the value "relation" of the property
    private LinkedList<String> relationLabels = new LinkedList<>();
    // Labels list for the value "value" of the property
    private LinkedList<String> valueLabels = new LinkedList<>();

    public PropertyLabelsDTO(Property property) {
        super(property);
        
        this.setRdfTypeLabels(property.getRdfTypeLabels());
        this.setRelationLabels(property.getRelationLabels());
        this.setValueLabels(property.getValueLabels());
    }

    @Override
    public Property createObjectFromDTO() {
        Property property = super.createObjectFromDTO();
        
        property.setRdfTypeLabels(rdfTypeLabels);
        property.setRelationLabels(relationLabels);
        property.setValueLabels(valueLabels);
        
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
    
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_SPECIES_HAS_SPECIES)
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
    
    public void setValueLabels(LinkedList<String> valueLabels) {
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

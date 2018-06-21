//******************************************************************************
//                                       PropertyVocabularyDTO.java
//
// Author(s): Morgane Vidal <morgane.vidal@inra.fr>
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: 19 juin 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  19 juin 2018
// Subject: represents the submitted and returned JSON for the vocabulary properties
//******************************************************************************
package phis2ws.service.resources.dto;

import io.swagger.annotations.ApiModelProperty;
import java.util.HashMap;
import java.util.Map;
import phis2ws.service.documentation.DocumentationAnnotation;
import phis2ws.service.resources.dto.manager.AbstractVerifiedClass;
import phis2ws.service.view.model.phis.Property;

/**
 * represents the submitted and returned JSON for the vocabulary properties
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class PropertyVocabularyDTO extends AbstractVerifiedClass {
    
    //relation name 
    //(e.g. http://www.phenome-fppn.fr/vocabulary/2017#fromVariety)
    private String relation;
    //the list of the labels of the property. Hash Map with the languages if needed
    //it is a hash map with the language and the label
    //if there is no language for a label, the key is equals to none (?)
    private HashMap<String, String> labels = new HashMap<>();
   
    @Override
    public Property createObjectFromDTO() {
        Property property = new Property();
        property.setRelation(relation);
        property.setLabels(labels);
        
        return property;
    }

    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_SPECIES_FROM_SPECIES)
    public String getRelation() {
        return relation;
    }

    public void setRelation(String relation) {
        this.relation = relation;
    }

    public HashMap<String, String> getLabels() {
        return labels;
    }

    public void setLabels(HashMap<String, String> labels) {
        this.labels = labels;
    }
    
    public void addLabel(String language, String label) {
        labels.put(language, label);
    }
}

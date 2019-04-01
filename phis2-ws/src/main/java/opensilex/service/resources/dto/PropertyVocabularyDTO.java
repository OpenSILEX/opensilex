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
package opensilex.service.resources.dto;

import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import opensilex.service.documentation.DocumentationAnnotation;
import opensilex.service.resources.dto.manager.AbstractVerifiedClass;
import opensilex.service.resources.validation.interfaces.URL;
import opensilex.service.view.model.Property;

/**
 * represents the submitted and returned JSON for the vocabulary properties
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class PropertyVocabularyDTO extends AbstractVerifiedClass {
    
    //relation name 
    //(e.g. http://www.opensilex.org/vocabulary/oeso#hasVariety)
    private String relation;
    //the list of the labels of the property. Hash Map with the languages if needed
    //it is a hash map with the language and the label
    //if there is no language for a label, the key is equals to none (?)
    private Map<String, Collection<String>> labels = new HashMap<>();
    //the list of the comments of the property. Map with the languages if knowned. 
    //if there is no language, the key will be "none"
    private Map<String, Collection<String>> comments = new HashMap<>();

    @Override
    public Property createObjectFromDTO() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @URL
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_SPECIES_HAS_SPECIES)
    public String getRelation() {
        return relation;
    }

    public void setRelation(String relation) {
        this.relation = relation;
    }

    public Map<String, Collection<String>> getLabels() {
        return labels;
    }

    public void setLabels(Map<String, Collection<String>> labels) {
        this.labels = labels;
    }
    
    public void addLabel(String language, String label) {
        Collection<String> labelsByLang = labels.get(language);
        if (labelsByLang == null) {
            labelsByLang = new ArrayList<>();
        }
        labelsByLang.add(label);
        labels.put(language, labelsByLang);
    }
    
    public Map<String, Collection<String>> getComments() {
        return comments;
    }

    public void setComments(Map<String, Collection<String>> comments) {
        this.comments = comments;
    }
}
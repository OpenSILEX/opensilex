//******************************************************************************
//                         PropertyVocabularyDTO.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 19 June 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.dto;

import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import opensilex.service.documentation.DocumentationAnnotation;
import opensilex.service.resource.dto.manager.AbstractVerifiedClass;
import opensilex.service.resource.validation.interfaces.URL;
import opensilex.service.model.Property;

/**
 * Vocabulary property DTO.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class PropertyVocabularyDTO extends AbstractVerifiedClass {
    
    /**
     * Relation name 
     * @example http://www.opensilex.org/vocabulary/oeso#hasVariety)
     */
    private String relation;
    private Map<String, Collection<String>> labels = new HashMap<>();
    
    /**
     * The list of the comments of the property. 
     * Map with the languages if known. 
     * if there is no language, the key will be "none".
     */
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
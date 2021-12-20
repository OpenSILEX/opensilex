/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.ontology.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.opensilex.sparql.ontology.dal.ClassModel;

import javax.validation.constraints.NotEmpty;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author vmigot
 */
public class RDFTypeTranslatedDTO extends RDFTypeDTO {

    @JsonProperty("name_translations")
    @NotEmpty
    protected Map<String, String> labelTranslations;

    @JsonProperty("comment_translations")
    @NotEmpty
    protected Map<String, String> commentTranslations;

    public Map<String, String> getLabelTranslations() {
        return labelTranslations;
    }

    public void setLabelTranslations(Map<String, String> labelTranslations) {
        this.labelTranslations = labelTranslations;
    }

    public Map<String, String> getCommentTranslations() {
        return commentTranslations;
    }

    public void setCommentTranslations(Map<String, String> commentTranslations) {
        this.commentTranslations = commentTranslations;
    }

    public RDFTypeTranslatedDTO(){

    }

    public RDFTypeTranslatedDTO(ClassModel model){
        super(model);
        setLabelTranslations(model.getLabel().getAllTranslations());
        if (model.getComment() != null) {
            setCommentTranslations(model.getComment().getAllTranslations());
        } else {
            setCommentTranslations(new HashMap<>());
        }
    }

}

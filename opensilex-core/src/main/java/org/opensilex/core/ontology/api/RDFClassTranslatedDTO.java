/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.ontology.api;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opensilex.core.ontology.dal.ClassModel;
import org.opensilex.core.ontology.dal.OwlRestrictionModel;
import org.opensilex.core.ontology.dal.PropertyModel;

/**
 *
 * @author vmigot
 */
public class RDFClassTranslatedDTO extends RDFClassDTO {

    protected Map<String, String> labelTranslations;

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

    public static RDFClassTranslatedDTO fromModel(RDFClassTranslatedDTO dto, ClassModel model) {
        RDFClassDTO.fromModel(dto, model);

        dto.setLabelTranslations(model.getLabel().getAllTranslations());
        if (model.getComment() != null) {
            dto.setCommentTranslations(model.getComment().getAllTranslations());
        } else {
            dto.setCommentTranslations(new HashMap<>());
        }

        return dto;
    }
}

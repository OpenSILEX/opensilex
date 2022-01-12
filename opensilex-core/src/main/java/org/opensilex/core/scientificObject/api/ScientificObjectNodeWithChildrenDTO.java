/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.scientificObject.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.opensilex.core.scientificObject.dal.ScientificObjectModel;

/**
 *
 * @author vmigot
 */

@JsonPropertyOrder({"uri", "rdf_type", "rdf_type_name", "name", "child_count", "creation_date", "destruction_date", "geometry"})
public class ScientificObjectNodeWithChildrenDTO extends ScientificObjectNodeDTO {

    @JsonProperty("child_count")
    protected Integer childCount;

    public Integer getChildCount() {
        return childCount;
    }

    public void setChildCount(Integer childCount) {
        this.childCount = childCount;
    }

    @Override
    public void fromModel(ScientificObjectModel model) {
        super.fromModel(model);
        this.setChildCount(model.getChildren().size());
    }

    public static ScientificObjectNodeWithChildrenDTO getDTOFromModel(ScientificObjectModel model) {
        ScientificObjectNodeWithChildrenDTO dto = new ScientificObjectNodeWithChildrenDTO();
        dto.fromModel(model);

        return dto;
    }
}

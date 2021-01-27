/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.scientificObject.api;

import org.opensilex.core.scientificObject.dal.ScientificObjectModel;

/**
 *
 * @author vmigot
 */
public class ScientificObjectNodeWithChildrenDTO extends ScientificObjectNodeDTO {

    private Integer childCount;

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

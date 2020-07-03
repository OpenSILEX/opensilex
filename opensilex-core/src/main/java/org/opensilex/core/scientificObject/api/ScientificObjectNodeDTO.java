/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.scientificObject.api;

import org.opensilex.core.scientificObject.dal.ScientificObjectModel;
import org.opensilex.sparql.response.NamedResourceDTO;

/**
 *
 * @author vmigot
 */
public class ScientificObjectNodeDTO extends NamedResourceDTO<ScientificObjectModel> {
    
    private boolean hasChildren;
    
    public boolean isHasChildren() {
        return hasChildren;
    }
    
    public void setHasChildren(boolean hasChildren) {
        this.hasChildren = hasChildren;
    }
    
    @Override
    public void toModel(ScientificObjectModel model) {
        super.toModel(model);
    }
    
    @Override
    public void fromModel(ScientificObjectModel model) {
        super.fromModel(model);
        this.setHasChildren(model.getChildren().size() > 0);
    }
    
    @Override
    public ScientificObjectModel newModelInstance() {
        return new ScientificObjectModel();
    }
    
    public static ScientificObjectNodeDTO getDTOFromModel(ScientificObjectModel model) {
        ScientificObjectNodeDTO dto = new ScientificObjectNodeDTO();
        dto.fromModel(model);
        
        return dto;
    }
}

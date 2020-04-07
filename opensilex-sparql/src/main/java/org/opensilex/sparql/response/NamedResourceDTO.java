/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql.response;

import org.opensilex.sparql.model.SPARQLNamedResourceModel;

/**
 *
 * @author vince
 */
public class NamedResourceDTO extends ResourceDTO {

    protected String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public static NamedResourceDTO fromModel(SPARQLNamedResourceModel model) {
        NamedResourceDTO dto = new NamedResourceDTO();
        dto.setUri(model.getUri());
        dto.setType(model.getType());
        dto.setTypeLabel(model.getTypeLabel().getDefaultValue());
        dto.setName(model.getName());
        return dto;
    }
}

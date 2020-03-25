/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.infrastructure.api;

import org.opensilex.core.infrastructure.dal.InfrastructureModel;
import org.opensilex.rest.sparql.dto.NamedResourceGetDTO;

/**
 *
 * @author vince
 */
public class InfrastructureGetDTO extends NamedResourceGetDTO {
 
    public static InfrastructureGetDTO fromModel(InfrastructureModel model) {
        InfrastructureGetDTO dto = new InfrastructureGetDTO();
        dto.setUri(model.getUri());
        dto.setType(model.getType());
        dto.setName(model.getName());
        
        return dto;
    }
}

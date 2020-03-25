/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.infrastructure.api;

import java.util.ArrayList;
import java.util.List;
import org.opensilex.core.infrastructure.dal.InfrastructureModel;
import org.opensilex.rest.sparql.dto.NamedResourceGetDTO;
import org.opensilex.rest.user.api.UserGetDTO;

/**
 *
 * @author vince
 */
public class InfrastructureGetDTO extends NamedResourceGetDTO {
    
    List<UserGetDTO> users;
    
    public List<UserGetDTO> getUsers() {
        return users;
    }
    
    public void setUsers(List<UserGetDTO> users) {
        this.users = users;
    }
    
    public static InfrastructureGetDTO fromModel(InfrastructureModel model) {
        InfrastructureGetDTO dto = new InfrastructureGetDTO();
        dto.setUri(model.getUri());
        dto.setType(model.getType());
        dto.setName(model.getName());
        
        List<UserGetDTO> users = new ArrayList<>();
        model.getUsers().forEach(user -> {
            users.add(UserGetDTO.fromModel(user));
        });
        dto.setUsers(users);
        
        return dto;
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.module.core.api.authentication;

import org.opensilex.module.core.dal.user.User;

/**
 *
 * @author vincent
 */
public class UserInfosDTO {

    private String name;

    private String email;
    
    private String token;
    
    UserInfosDTO(User user) {
        this.name = user.getName();
        
        this.token = user.computeToken();
        
        this.email = user.getEmail().getAddress();
    }
    
}

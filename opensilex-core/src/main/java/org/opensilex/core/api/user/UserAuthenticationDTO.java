/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.api.user;

import org.opensilex.server.validation.Required;

import io.swagger.annotations.ApiModelProperty;

/**
 *
 * @author vincent
 */
public class UserAuthenticationDTO {

    private String identifier;

    private String password;

    @Required
    @ApiModelProperty(example = "john.doe@example.com")
    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    @Required
    @ApiModelProperty(example = "azerty")
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}

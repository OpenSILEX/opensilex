/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.module.core.api.authentication;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.annotations.ApiModelProperty;
import javax.mail.internet.InternetAddress;
import org.opensilex.utils.deserializer.EmailDeserializer;
import org.opensilex.server.rest.validation.interfaces.Required;

/**
 *
 * @author vincent
 */
public class UserAuthDTO {
    private InternetAddress email;
    
    private String password;

    @Required
    @JsonDeserialize(using = EmailDeserializer.class)
    @ApiModelProperty(example = "admin@opensilex.org")
    public InternetAddress getEmail() {
        return email;
    }

    public void setEmail(InternetAddress email) {
        this.email = email;
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

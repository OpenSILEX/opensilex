/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package phis2ws.service.resources.dto;

import io.swagger.annotations.ApiModelProperty;
import phis2ws.service.documentation.DocumentationAnnotation;
import phis2ws.service.resources.dto.validation.interfaces.Required;
import phis2ws.service.resources.dto.manager.AbstractVerifiedClass;

/**
 * Represente le JSON soumis pour les objets de type token
 *
 * @author A. Charleroy
 */
public class TokenDTO extends AbstractVerifiedClass {

    private String grant_type;
    private String username;
    private String password;
    private String client_id;

    // Uncomment if you use jwt
    // @ApiModelProperty(example = EXAMPLE_TOKEN_JWT_GRANTTYPE)
    @Required(message = "Wrong grant type")
    @ApiModelProperty(example = "password")
    public String getGrant_type() {
        return grant_type;
    }

    public void setGrant_type(String grant_type) {
        this.grant_type = grant_type;
    }

    @Required(message = "Username must be filled")
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_USER_EMAIL)
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
    
    
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_USER_PASSWORD)
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    // Uncomment if you use jwt
    // @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_TOKEN_JWT_CLIENTID)
    public String getClient_id() {
        return client_id;
    }

    public void setClient_id(String client_id) {
        this.client_id = client_id;
    }

    @Override
    public Object createObjectFromDTO() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}

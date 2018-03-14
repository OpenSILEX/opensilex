/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package phis2ws.service.resources.dto;

import io.swagger.annotations.ApiModelProperty;
import java.util.HashMap;
import java.util.Map;
import phis2ws.service.documentation.DocumentationAnnotation;
import phis2ws.service.resources.dto.manager.AbstractVerifiedClass;

/**
 *  Represente le JSON soumis pour les objets de type token
 * @author A. Charleroy
 */
public class TokenDTO extends AbstractVerifiedClass {

    private String grant_type;
    private String username;
    private String password;
    private String client_id;

    @ApiModelProperty(example = "password")
    public String getGrant_type() {
        return grant_type;
    }

    public void setGrant_type(String grant_type) {
        this.grant_type = grant_type;
    }

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

   // @ApiModelProperty(example = "eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJndWVzdHBoaXNAc3VwYWdyby5pbnJhLmZyIiwiaXNzIjoiR25wSVMiLCJpYXQiOjE0NzYyNzY5MzgsImV4cCI6MTc5MTgwOTczOH0.Tm0fqmqSp1tna9g5govzgfPiRLaEu6OC5mvUNf1DKxa1nnAp5N1QUYwtTFdgOcUoWoO9nwj6Fe-4YbKpe7ECuNLqhm3-Pu42YI2359AkEorMxTfg4VUJfUVljgxSIakqrfCHt1qRImTpQegQiPsZque_BXBUxU3I7rScNODfzPs")
    public String getClient_id() {
        return client_id;
    }

    public void setClient_id(String client_id) {
        this.client_id = client_id;
    }

    @Override
    public Map rules() {
        Map<String, Boolean> rules = new HashMap<>();
        rules.put("grant_type", Boolean.FALSE);
        rules.put("username", Boolean.TRUE);
        rules.put("password", Boolean.FALSE);
        rules.put("client_id", Boolean.FALSE);
        return rules;
    }

    @Override
    public Object createObjectFromDTO() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}

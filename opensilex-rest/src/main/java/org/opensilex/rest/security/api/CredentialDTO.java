/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.rest.security.api;

import io.swagger.annotations.ApiModelProperty;
import org.opensilex.rest.validation.Required;

/**
 *
 * @author vidalmor
 */
public class CredentialDTO {

    private String id;

    private String label;

    @Required
    @ApiModelProperty(value = "Credential identifier")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Required
    @ApiModelProperty(value = "Credential label")
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

}

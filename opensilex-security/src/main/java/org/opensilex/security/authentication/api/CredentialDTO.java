/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.security.authentication.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.server.rest.validation.Required;

/**
 *
 * @author vidalmor
 */
@JsonPropertyOrder({"id","name"})
public class CredentialDTO {

    private String id;
    
    @JsonProperty("name")
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
    @ApiModelProperty(value = "Credential name")
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

}

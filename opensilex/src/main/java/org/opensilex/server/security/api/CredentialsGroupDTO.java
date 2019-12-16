/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.server.security.api;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.Map;

/**
 *
 * @author vince
 */
@ApiModel
public class CredentialsGroupDTO {

    private String groupId;

    private Map<String, String> credentials;

    @ApiModelProperty(value = "Credential group identifier", example = "Security")
    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    @ApiModelProperty(value = "Credentials Map", dataType = "Map[string,string]", reference = "Map")
    public Map<String, String> getCredentials() {
        return credentials;
    }

    public void setCredentials(Map<String, String> credentials) {
        this.credentials = credentials;
    }

}

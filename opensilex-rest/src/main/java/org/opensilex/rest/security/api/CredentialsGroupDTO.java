/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.rest.security.api;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import javax.validation.Valid;

/**
 *
 * @author vince
 */
@ApiModel
public class CredentialsGroupDTO {

    private String groupId;

    private List<CredentialDTO> credentials;

    @ApiModelProperty(value = "Credential group identifier", example = "Security")
    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    @Valid()
    @ApiModelProperty(value = "Credentials Map",  dataType = "List[org.opensilex.rest.security.api.CredentialDTO]", reference = "List")
    public List<CredentialDTO> getCredentials() {
        return credentials;
    }

    public void setCredentials(List<CredentialDTO> credentials) {
        this.credentials = credentials;
    }

}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.rest.group.api;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.net.URI;
import java.util.List;
import javax.validation.constraints.Email;
import org.opensilex.rest.validation.Required;
import org.opensilex.rest.validation.ValidURI;

/**
 *
 * @author vidalmor
 */
@ApiModel
public class GroupCreationDTO extends GroupGetDTO {

    @ValidURI
    public URI getUri() {
        return super.getUri();
    }

    @Required
    @ApiModelProperty(required = true)
    public String getName() {
        return super.getName();
    }

    @ValidURI
    public List<URI> getProfiles() {
        return super.getProfiles();
    }

    @ValidURI
    public List<URI> getUsers() {
        return super.getUsers();
    }
}

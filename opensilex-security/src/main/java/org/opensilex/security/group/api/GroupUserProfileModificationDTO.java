/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.security.group.api;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.net.URI;
import javax.validation.constraints.NotNull;
import org.opensilex.server.rest.validation.ValidURI;

/**
 *
 * @author vidalmor
 */
@ApiModel
public class GroupUserProfileModificationDTO extends GroupUserProfileDTO {

    @ValidURI
    @NotNull
    @ApiModelProperty(value = "User associated profile URI", required = true)
    public URI getProfileURI() {
        return this.profileURI;
    }

    @ValidURI
    @NotNull
    @ApiModelProperty(value = "User URI", required = true)
    public URI getUserURI() {
        return this.userURI;
    }
}

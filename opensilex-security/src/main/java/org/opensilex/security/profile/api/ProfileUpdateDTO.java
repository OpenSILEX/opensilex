//******************************************************************************
//                          UserCreationDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.security.profile.api;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.net.URI;
import java.util.List;
import javax.validation.constraints.NotNull;
import org.opensilex.server.rest.validation.Required;
import org.opensilex.server.rest.validation.ValidURI;

/**
 * <pre>
 * DTO repensenting JSON for user creation.
 *
 * JSON representation:
 * {
 *      email: ... user email
 *      firstName: ... user first name
 *      lastName: ... user last name
 *      admin: ... flag to define if user is an admin or not
 *      password: ... optional user password to update
 * }
 * </pre>
 *
 * @see org.opensilex.rest.profile.api.ProfileAPI#updateProfile(org.opensilex.rest.profile.api.ProfileUpdateDTO) 
 * @author Vincent Migot
 */
@ApiModel
public class ProfileUpdateDTO extends ProfileGetDTO {

    @ValidURI
    @NotNull
    @ApiModelProperty(value = "User URI", example = "http://opensilex.dev/users#agent.Admin_OpenSilex", required = true)
    @Override
    public URI getUri() {
        return uri;
    }

    @Required
    @ApiModelProperty(value = "Profile name", example = "profile1", required = true)
    @Override
    public String getName() {
        return name;
    }

    @NotNull
    @ApiModelProperty(value = "Profile credentials", required = true)
    @Override
    public List<String> getCredentials() {
        return credentials;
    }
}

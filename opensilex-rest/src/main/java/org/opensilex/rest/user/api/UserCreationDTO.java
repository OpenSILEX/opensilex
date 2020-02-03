//******************************************************************************
//                          UserCreationDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.rest.user.api;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.net.URI;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import org.opensilex.rest.validation.Required;
import org.opensilex.rest.validation.ValidURI;

/**
 * <pre>
 * DTO repensenting JSON for user creation.
 *
 * JSON representation:
 * {
 *      uri: ... optional custom uri, auto-generated if missing
 *      email: ... user email
 *      firstName: ... user first name
 *      lastName: ... user last name
 *      admin: ... flag to define if user is an admin or not
 *      password: ... user password
 * }
 * </pre>
 *
 * @see org.opensilex.rest.user.api.UserAPI#createUser(org.opensilex.rest.user.api.UserCreationDTO, javax.ws.rs.core.SecurityContext) 
 * @author Vincent Migot
 */
@ApiModel
public class UserCreationDTO extends UserGetDTO {

    /**
     * User password
     */
    protected String password;

    @ValidURI
    public URI getUri() {
        return super.getUri();
    }

    @Email
    @Required
    @ApiModelProperty(required = true)
    public String getEmail() {
        return super.getEmail();
    }

    @Required
    @ApiModelProperty(required = true)
    public String getFirstName() {
        return super.getFirstName();
    }

    @Required
    @ApiModelProperty(required = true)
    public String getLastName() {
        return super.getLastName();
    }

    @Required
    @ApiModelProperty(value = "User password", example = "azerty", required = true)
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @NotNull
    @ApiModelProperty(value = "User admin flag", example = "false", required = true)
    public boolean isAdmin() {
        return super.isAdmin();
    }
}

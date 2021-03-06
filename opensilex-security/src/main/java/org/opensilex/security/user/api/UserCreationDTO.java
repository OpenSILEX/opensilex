//******************************************************************************
//                          UserCreationDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.security.user.api;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.net.URI;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import org.opensilex.OpenSilex;
import org.opensilex.server.rest.validation.Required;
import org.opensilex.server.rest.validation.ValidURI;

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
 * @author Vincent Migot
 */
@ApiModel
@JsonPropertyOrder({"uri", "first_name", "last_name", "email", "language", "password",
    "admin"})
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
    
    @NotNull
    @ApiModelProperty(value = "User language", example = OpenSilex.DEFAULT_LANGUAGE, required = true)
    public String getLanguage() {
        return super.getLanguage();
    }
}

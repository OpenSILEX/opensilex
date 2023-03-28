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

    protected String password;

    @Override
    @ValidURI
    @ApiModelProperty(value = "Account URI", example = "http://opensilex.dev/users#jean.michel.inrae")
    public URI getUri() {
        return super.getUri();
    }

    @Override
    @Email
    @Required
    @ApiModelProperty(value = "User email", example = "jean.michel@example.com", required = true)
    public String getEmail() {
        return super.getEmail();
    }

    @Override
    @Required
    @ApiModelProperty(value = "Person first name", example = "Jean", required = true)
    public String getFirstName() {
        return super.getFirstName();
    }

    @Override
    @Required
    @ApiModelProperty(value = "Person last name", example = "Michel", required = true)
    public String getLastName() {
        return super.getLastName();
    }

    @Required
    @ApiModelProperty(value = "Account password", example = "azerty", required = true)
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    @NotNull
    @ApiModelProperty(value = "Account admin flag", example = "false", required = true)
    public boolean isAdmin() {
        return super.isAdmin();
    }

    @Override
    @NotNull
    @ApiModelProperty(value = "Account language", example = OpenSilex.DEFAULT_LANGUAGE, required = true)
    public String getLanguage() {
        return super.getLanguage();
    }
}

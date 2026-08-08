//******************************************************************************
//                          UserCreationDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.security.user.api;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema
@JsonPropertyOrder({"uri", "first_name", "last_name", "email", "language", "password",
    "admin"})
public class UserCreationDTO extends UserGetDTO {

    protected String password;

    @Override
    @ValidURI
    @Schema(description = "Account URI", example = "http://opensilex.dev/users#jean.michel.inrae")
    public URI getUri() {
        return super.getUri();
    }

    @Override
    @Email
    @Required
    @Schema(description = "User email", example = "jean.michel@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    public String getEmail() {
        return super.getEmail();
    }

    @Override
    @Required
    @Schema(description = "Person first name", example = "Jean", requiredMode = Schema.RequiredMode.REQUIRED)
    public String getFirstName() {
        return super.getFirstName();
    }

    @Override
    @Required
    @Schema(description = "Person last name", example = "Michel", requiredMode = Schema.RequiredMode.REQUIRED)
    public String getLastName() {
        return super.getLastName();
    }

    @Required
    @Schema(description = "Account password", example = "azerty", requiredMode = Schema.RequiredMode.REQUIRED)
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    @NotNull
    @Schema(description = "Account admin flag", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
    public boolean isAdmin() {
        return super.isAdmin();
    }

    @Override
    @NotNull
    @Schema(description = "Account language", example = OpenSilex.DEFAULT_LANGUAGE, requiredMode = Schema.RequiredMode.REQUIRED)
    public String getLanguage() {
        return super.getLanguage();
    }
}

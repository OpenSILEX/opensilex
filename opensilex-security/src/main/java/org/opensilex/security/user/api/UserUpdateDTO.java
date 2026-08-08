//******************************************************************************
//                          UserCreationDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.security.user.api;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import org.opensilex.OpenSilex;
import org.opensilex.server.rest.validation.NullOrNotEmpty;
import org.opensilex.server.rest.validation.Required;
import org.opensilex.server.rest.validation.ValidURI;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import java.net.URI;
import java.util.List;

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
 * @author Vincent Migot
 */
@Schema
@JsonPropertyOrder({"uri", "first_name", "language", "password",
    "admin", "last_name", "email", "holderOfTheAccountURI"})
public class UserUpdateDTO extends UserGetDTO {

    protected String password;

    @ValidURI
    @NotNull
    public URI getUri() {
        return super.getUri();
    }

    @Email
    @Required
    @Schema(description = "User email", example = "jean.michel@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    public String getEmail() {
        return super.getEmail();
    }

    @Schema(description = "User first name", example = "Janne", requiredMode = Schema.RequiredMode.REQUIRED)
    public String getFirstName() {
        return super.getFirstName();
    }

    @Schema(description = "User last name", example = "Michelle", requiredMode = Schema.RequiredMode.REQUIRED)
    public String getLastName() {
        return super.getLastName();
    }

    @Schema()
    public List<URI> getFavorites() {
        return super.getFavorites();
    }

    @NullOrNotEmpty
    @Schema(description = "Optional user password", example = "azerty")
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @NotNull
    @Schema(description = "User admin flag", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
    public boolean isAdmin() {
        return super.isAdmin();
    }

    @NotNull
    @Schema(description = "User language", example = OpenSilex.DEFAULT_LANGUAGE, requiredMode = Schema.RequiredMode.REQUIRED)
    public String getLanguage() {
        return super.getLanguage();
    }
}

//******************************************************************************
//                          UserCreationDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.security.profile.api;

import io.swagger.v3.oas.annotations.media.Schema;
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
 * @author Vincent Migot
 */
@Schema
public class ProfileUpdateDTO extends ProfileGetDTO {

    @ValidURI
    @NotNull
    @Schema(description = "User URI", example = "http://opensilex.dev/users#agent.Admin_OpenSilex", requiredMode = Schema.RequiredMode.REQUIRED)
    @Override
    public URI getUri() {
        return uri;
    }

    @Required
    @Schema(description = "Profile name", example = "profile1", requiredMode = Schema.RequiredMode.REQUIRED)
    @Override
    public String getName() {
        return name;
    }

    @NotNull
    @Schema(description = "Profile credentials", requiredMode = Schema.RequiredMode.REQUIRED)
    @Override
    public List<String> getCredentials() {
        return credentials;
    }
}

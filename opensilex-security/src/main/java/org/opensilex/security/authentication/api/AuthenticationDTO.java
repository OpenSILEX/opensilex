//******************************************************************************
//                          AuthenticationDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.security.authentication.api;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.server.rest.validation.Required;

import static org.opensilex.security.SecurityModule.DEFAULT_SUPER_ADMIN_EMAIL;
import static org.opensilex.security.SecurityModule.DEFAULT_SUPER_ADMIN_PASSWORD;

/**
 * <pre>
 * Authentication DTO
 *
 * JSON model:
 * {
 *      identifier: ...user email
 *      password: ...user password
 * }
 * </pre>
 *
 * @author Vincent Migot
 */
@ApiModel
public class AuthenticationDTO {

    /**
     * Authentication identifier
     */
    private String identifier;

    /**
     * Authentication password
     */
    private String password;

    @Required
    @ApiModelProperty(value = "User identifier, email or URI", example = DEFAULT_SUPER_ADMIN_EMAIL)
    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    @Required
    @ApiModelProperty(value = "User password", example = DEFAULT_SUPER_ADMIN_PASSWORD)
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}

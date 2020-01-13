//******************************************************************************
//                          AuthenticationDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.rest.security.api;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.rest.validation.Required;

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
 * @see
 * org.opensilex.server.security.api.SecurityAPI#authenticate(org.opensilex.server.security.api.AuthenticationDTO)
 * @author vincent
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
    @ApiModelProperty(value = "User identifier, email or URI", example = "admin@opensilex.org")
    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    @Required
    @ApiModelProperty(value = "User password", example = "admin")
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}

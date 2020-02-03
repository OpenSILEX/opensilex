//******************************************************************************
//                          TokenGetDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.rest.security.api;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * <pre>
 * DTO repensenting JSON for getting or renew user token.
 *
 * JSON representation:
 * {
 *      token: ... user token
 * }
 * </pre>
 *
<<<<<<< HEAD
=======
 * @see org.opensilex.rest.security.api.SecurityAPI#authenticate(org.opensilex.rest.security.api.AuthenticationDTO) 
 * @see org.opensilex.rest.security.api.SecurityAPI#renewToken(java.lang.String, javax.ws.rs.core.SecurityContext) 
>>>>>>> 1123af87d11f910595886240940fbbfe7a99952f
 * @author Vincent Migot
 * @see org.opensilex.server.security.api.SecurityAPI#authenticate(org.opensilex.server.security.api.AuthenticationDTO)
 * @see org.opensilex.server.security.api.SecurityAPI#renewToken(java.lang.String,
 * javax.ws.rs.core.SecurityContext)
 */
@ApiModel
public class TokenGetDTO {

    protected String token;

    @ApiModelProperty(value = "User token", example = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzUxMiJ9.eyJpc19hZG1pbiI6dHJ...")
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public TokenGetDTO(String token) {
        this.token = token;
    }

    public TokenGetDTO() {
    }
}

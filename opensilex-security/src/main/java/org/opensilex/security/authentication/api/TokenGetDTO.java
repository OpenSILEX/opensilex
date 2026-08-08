//******************************************************************************
//                          TokenGetDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.security.authentication.api;

import io.swagger.v3.oas.annotations.media.Schema;

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
 * @author Vincent Migot
 */
@Schema
public class TokenGetDTO {

    protected String token;

    public TokenGetDTO(){

    }

    public TokenGetDTO(String token) {
        this.token = token;
    }

    @Schema(description = "User token", example = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzUxMiJ9.eyJpc19hZG1pbiI6dHJ...")
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
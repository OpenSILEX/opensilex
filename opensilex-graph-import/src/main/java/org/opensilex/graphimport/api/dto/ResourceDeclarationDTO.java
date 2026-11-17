//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * A resource to declare in an import, or to change when its identifier is already there.
 *
 * @author Arnaud Charleroy
 */
public class ResourceDeclarationDTO {

    @NotNull
    @JsonProperty("session_id")
    @ApiModelProperty(value = "The import", required = true)
    private String sessionId;

    @Valid
    @NotNull
    @JsonProperty("resource")
    @ApiModelProperty(value = "The resource", required = true)
    private DeclaredResourceDTO resource;

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public DeclaredResourceDTO getResource() {
        return resource;
    }

    public void setResource(DeclaredResourceDTO resource) {
        this.resource = resource;
    }
}

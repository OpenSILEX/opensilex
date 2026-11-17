//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotNull;
import java.net.URI;

/**
 * The provenance the data of an import are created with: chosen once for the whole import.
 *
 * @author Arnaud Charleroy
 */
public class ImportProvenanceDTO {

    @NotNull
    @JsonProperty("session_id")
    @ApiModelProperty(value = "The import", required = true)
    private String sessionId;

    @JsonProperty("provenance")
    @ApiModelProperty(value = "The provenance; absent to choose none")
    private URI provenance;

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public URI getProvenance() {
        return provenance;
    }

    public void setProvenance(URI provenance) {
        this.provenance = provenance;
    }
}

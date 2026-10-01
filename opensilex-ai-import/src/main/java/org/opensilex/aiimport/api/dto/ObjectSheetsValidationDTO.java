//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.net.URI;

/**
 * A request to check the object sheets against the platform, without writing anything.
 *
 * @author Arnaud Charleroy
 */
public class ObjectSheetsValidationDTO {

    @NotEmpty
    @JsonProperty("session_id")
    private String sessionId;

    @NotNull
    private URI experiment;

    @ApiModelProperty(value = "Identifier of the open conversation", required = true)
    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    @ApiModelProperty(value = "The experiment the objects would belong to", required = true)
    public URI getExperiment() {
        return experiment;
    }

    public void setExperiment(URI experiment) {
        this.experiment = experiment;
    }
}

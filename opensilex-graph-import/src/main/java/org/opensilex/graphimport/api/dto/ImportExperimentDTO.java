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
 * The experiment whose scientific objects an import may recognise.
 *
 * @author Arnaud Charleroy
 */
public class ImportExperimentDTO {

    @NotNull
    @JsonProperty("session_id")
    @ApiModelProperty(value = "The import", required = true)
    private String sessionId;

    @JsonProperty("experiment")
    @ApiModelProperty(value = "The experiment; absent to choose none")
    private URI experiment;

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public URI getExperiment() {
        return experiment;
    }

    public void setExperiment(URI experiment) {
        this.experiment = experiment;
    }
}

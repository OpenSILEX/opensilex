//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * A step of the creation to check or to run. The number of resources the user saw guards against a
 * plan that changed in between: the step runs only if it would still create that many.
 *
 * @author Arnaud Charleroy
 */
public class CreationRequestDTO {

    @NotNull
    @JsonProperty("session_id")
    @ApiModelProperty(value = "The import", required = true)
    private String sessionId;

    @NotNull
    @JsonProperty("step")
    @ApiModelProperty(value = "The step: experiments, factors…", required = true)
    private String step;

    @NotNull
    @Min(0)
    @JsonProperty("expected")
    @ApiModelProperty(value = "How many resources the user saw ready", required = true, example = "1")
    private Integer expected;

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getStep() {
        return step;
    }

    public void setStep(String step) {
        this.step = step;
    }

    public Integer getExpected() {
        return expected;
    }

    public void setExpected(Integer expected) {
        this.expected = expected;
    }
}

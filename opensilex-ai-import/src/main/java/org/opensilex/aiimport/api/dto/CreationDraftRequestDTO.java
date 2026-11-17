//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotNull;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Which creation to draft, in which conversation.
 *
 * @author Arnaud Charleroy
 */
public class CreationDraftRequestDTO {

    @JsonProperty("session_id")
    private String sessionId;

    private String target;

    private Map<String, String> values = new LinkedHashMap<>();

    @NotNull
    @ApiModelProperty(value = "The conversation the draft is made in", required = true)
    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    @NotNull
    @ApiModelProperty(value = "What to create: PROJECT, EXPERIMENT, FACTORS, SCIENTIFIC_OBJECTS, VARIABLE, EVENT "
            + "or DATA", required = true, example = "SCIENTIFIC_OBJECTS")
    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    @ApiModelProperty(value = "Values the user already chose, by field name: they win over what the files say")
    public Map<String, String> getValues() {
        return values;
    }

    public void setValues(Map<String, String> values) {
        this.values = values;
    }
}

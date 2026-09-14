//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotEmpty;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Confirmation of a draft the assistant proposed.
 * <p>
 * The draft is named rather than described: the user confirms what they were shown, and the server
 * revalidates it against the current state of the instance before writing. Corrections made on the
 * card travel in {@code values}.
 * <p>
 * The conversation identifier travels in the body for the same reason as elsewhere: the client
 * generator cannot express a body alongside a path parameter.
 *
 * @author Arnaud Charleroy
 */
public class CreationRequestDTO {

    @NotEmpty
    @JsonProperty("session_id")
    private String sessionId;

    @NotEmpty
    @JsonProperty("proposal_id")
    private String proposalId;

    private Map<String, String> values = new LinkedHashMap<>();

    @ApiModelProperty(value = "Identifier of the open conversation", required = true)
    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    @ApiModelProperty(value = "Identifier of the draft the assistant proposed", required = true)
    public String getProposalId() {
        return proposalId;
    }

    public void setProposalId(String proposalId) {
        this.proposalId = proposalId;
    }

    @ApiModelProperty(value = "The field values as shown on the draft, with any correction the user "
            + "made. Omitted fields keep the drafted value.")
    public Map<String, String> getValues() {
        return values;
    }

    public void setValues(Map<String, String> values) {
        this.values = values;
    }
}

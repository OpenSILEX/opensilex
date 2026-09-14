//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

/**
 * A question asked by the user, about one open conversation.
 * <p>
 * The conversation identifier travels in the body rather than in the path: the TypeScript client
 * generator cannot express a request that has both a body and a path parameter, so every endpoint
 * in this instance that carries a body keeps its identifiers inside it.
 *
 * @author Arnaud Charleroy
 */
public class ChatQuestionDTO {

    /**
     * Bounded so that a single message cannot be used to push an arbitrary amount of text through
     * to the language model.
     */
    public static final int MAX_LENGTH = 4000;

    @NotEmpty
    @JsonProperty("session_id")
    private String sessionId;

    @NotEmpty
    @Size(max = MAX_LENGTH)
    private String content;

    @ApiModelProperty(value = "Identifier of the open conversation", required = true)
    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    @ApiModelProperty(value = "The question", example = "Quelles variables manquent pour cet import ?",
            required = true)
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}

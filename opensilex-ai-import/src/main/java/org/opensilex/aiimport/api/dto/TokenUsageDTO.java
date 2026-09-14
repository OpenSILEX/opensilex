//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.aiimport.service.TokenUsage;

/**
 * What a conversation has cost, as the language model endpoint reported it.
 *
 * @author Arnaud Charleroy
 */
public class TokenUsageDTO {

    @JsonProperty("prompt_tokens")
    private long promptTokens;

    @JsonProperty("completion_tokens")
    private long completionTokens;

    @JsonProperty("total_tokens")
    private long totalTokens;

    private int calls;

    public static TokenUsageDTO fromModel(TokenUsage model) {
        TokenUsageDTO dto = new TokenUsageDTO();
        dto.promptTokens = model.getPromptTokens();
        dto.completionTokens = model.getCompletionTokens();
        dto.totalTokens = model.getTotalTokens();
        dto.calls = model.getCalls();
        return dto;
    }

    @ApiModelProperty(value = "Input tokens billed across the whole conversation")
    public long getPromptTokens() {
        return promptTokens;
    }

    public void setPromptTokens(long promptTokens) {
        this.promptTokens = promptTokens;
    }

    public long getCompletionTokens() {
        return completionTokens;
    }

    public void setCompletionTokens(long completionTokens) {
        this.completionTokens = completionTokens;
    }

    public long getTotalTokens() {
        return totalTokens;
    }

    public void setTotalTokens(long totalTokens) {
        this.totalTokens = totalTokens;
    }

    @ApiModelProperty(value = "API calls made. One user message can be several, because the "
            + "assistant looks things up before answering")
    public int getCalls() {
        return calls;
    }

    public void setCalls(int calls) {
        this.calls = calls;
    }
}

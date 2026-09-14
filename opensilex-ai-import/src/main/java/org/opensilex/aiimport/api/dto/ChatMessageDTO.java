//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.aiimport.service.AiImportMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * One turn of the conversation.
 *
 * @author Arnaud Charleroy
 */
public class ChatMessageDTO {

    private String role;
    private String content;

    @JsonProperty("content_key")
    private String contentKey;
    private String createdAt;
    private List<String> lookups = new ArrayList<>();

    @com.fasterxml.jackson.annotation.JsonProperty("proposal_id")
    private String proposalId;

    public static ChatMessageDTO fromModel(AiImportMessage model) {
        ChatMessageDTO dto = new ChatMessageDTO();
        dto.role = model.getRole();
        dto.content = model.getContent();
        dto.contentKey = model.getContentKey();
        dto.createdAt = String.valueOf(model.getCreatedAt());
        dto.lookups = model.getLookups();
        dto.proposalId = model.getProposalId();
        return dto;
    }

    @ApiModelProperty(value = "user or assistant", example = "assistant")
    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    @ApiModelProperty(value = "The message, as markdown")
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getContentKey() {
        return contentKey;
    }

    public void setContentKey(String contentKey) {
        this.contentKey = contentKey;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    @ApiModelProperty(value = "The lookups performed while producing this answer, so a claim about "
            + "the instance can be traced back to a query")
    public List<String> getLookups() {
        return lookups;
    }

    public void setLookups(List<String> lookups) {
        this.lookups = lookups;
    }

    @ApiModelProperty(value = "The creation drafted during this turn, whose card renders under it")
    public String getProposalId() {
        return proposalId;
    }

    public void setProposalId(String proposalId) {
        this.proposalId = proposalId;
    }
}

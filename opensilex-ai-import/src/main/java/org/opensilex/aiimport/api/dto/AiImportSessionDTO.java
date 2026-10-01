//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.aiimport.create.CreationProposal;
import org.opensilex.aiimport.mapping.ColumnMapping;
import org.opensilex.aiimport.service.AiImportMessage;
import org.opensilex.aiimport.service.AiImportSession;

import java.util.ArrayList;
import java.util.List;

/**
 * One conversation about one uploaded file.
 *
 * @author Arnaud Charleroy
 */
public class AiImportSessionDTO {

    @JsonProperty("session_id")
    private String sessionId;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("profile_id")
    private String profileId;

    @JsonProperty("file_name")
    private String fileName;

    private WorkbookStructureDTO structure;

    private ResolutionReportDTO report;

    private List<ChatMessageDTO> messages = new ArrayList<>();

    private List<ColumnMappingDTO> mapping = new ArrayList<>();

    @JsonProperty("token_usage")
    private TokenUsageDTO tokenUsage;

    @JsonProperty("pending_proposal")
    private CreationProposalDTO pendingProposal;

    /**
     * Every draft of the conversation, applied and cancelled included, so that a resumed
     * conversation shows each card under the message that made it.
     */
    private List<CreationProposalDTO> proposals = new ArrayList<>();

    public static AiImportSessionDTO fromModel(AiImportSession model, int sampleRows) {
        return fromModel(model, sampleRows, null);
    }

    /**
     * @param proposalRequirements the requirements matching the pending draft, so its card can show
     *                             a label and a kind per field. Null when there is no draft.
     */
    public static AiImportSessionDTO fromModel(AiImportSession model, int sampleRows,
                                               org.opensilex.aiimport.create.CreationRequirements
                                                       proposalRequirements) {
        AiImportSessionDTO dto = new AiImportSessionDTO();
        dto.sessionId = model.getId();
        dto.createdAt = String.valueOf(model.getCreatedAt());
        dto.profileId = model.getProfileId();
        dto.fileName = model.getFileName();
        if (model.getWorkbook() != null) {
            dto.structure = WorkbookStructureDTO.fromModel(model.getWorkbook(), sampleRows);
        }
        if (model.getReport() != null) {
            dto.report = ResolutionReportDTO.fromModel(model.getReport());
        }
        for (AiImportMessage message : model.getTranscript()) {
            dto.messages.add(ChatMessageDTO.fromModel(message));
        }
        for (ColumnMapping columnMapping : model.getMappings()) {
            dto.mapping.add(ColumnMappingDTO.fromModel(columnMapping));
        }
        dto.tokenUsage = TokenUsageDTO.fromModel(model.getTokenUsage());
        if (model.getPendingProposal() != null) {
            dto.pendingProposal = CreationProposalDTO.fromModel(
                    model.getPendingProposal(), proposalRequirements);
        }
        for (CreationProposal proposal : model.getProposals()) {
            dto.proposals.add(proposal == model.getPendingProposal()
                    ? dto.pendingProposal
                    : CreationProposalDTO.fromModel(proposal, null));
        }
        return dto;
    }

    @ApiModelProperty(value = "Identifier to pass on the following calls")
    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    @ApiModelProperty(value = "The file family that was recognised", example = "vitis-explorer")
    public String getProfileId() {
        return profileId;
    }

    public void setProfileId(String profileId) {
        this.profileId = profileId;
    }

    public WorkbookStructureDTO getStructure() {
        return structure;
    }

    public void setStructure(WorkbookStructureDTO structure) {
        this.structure = structure;
    }

    public ResolutionReportDTO getReport() {
        return report;
    }

    public void setReport(ResolutionReportDTO report) {
        this.report = report;
    }

    public List<ChatMessageDTO> getMessages() {
        return messages;
    }

    public void setMessages(List<ChatMessageDTO> messages) {
        this.messages = messages;
    }

    @ApiModelProperty(value = "How each column was mapped, and where the values disagree with it")
    public List<ColumnMappingDTO> getMapping() {
        return mapping;
    }

    public void setMapping(List<ColumnMappingDTO> mapping) {
        this.mapping = mapping;
    }

    @ApiModelProperty(value = "What this conversation has cost, as the endpoint reported it")
    public TokenUsageDTO getTokenUsage() {
        return tokenUsage;
    }

    public void setTokenUsage(TokenUsageDTO tokenUsage) {
        this.tokenUsage = tokenUsage;
    }

    @ApiModelProperty(value = "The creation awaiting the user's confirmation, if any")
    public CreationProposalDTO getPendingProposal() {
        return pendingProposal;
    }

    public void setPendingProposal(CreationProposalDTO pendingProposal) {
        this.pendingProposal = pendingProposal;
    }

    @ApiModelProperty(value = "Every draft of the conversation, in the order they were made")
    public List<CreationProposalDTO> getProposals() {
        return proposals;
    }

    public void setProposals(List<CreationProposalDTO> proposals) {
        this.proposals = proposals;
    }

    @ApiModelProperty(example = "Vitis_2020.xlsx")
    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}

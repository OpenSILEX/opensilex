//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.aiimport.service.store.SavedSessionSummary;

import java.time.Duration;
import java.time.Instant;

/**
 * A stored import conversation the user can resume.
 *
 * @author Arnaud Charleroy
 */
public class SavedSessionDTO {

    @JsonProperty("session_id")
    private String sessionId;

    @JsonProperty("file_name")
    private String fileName;

    @JsonProperty("profile_id")
    private String profileId;

    @JsonProperty("created_at")
    private Instant createdAt;

    @JsonProperty("updated_at")
    private Instant updatedAt;

    @JsonProperty("message_count")
    private int messageCount;

    @JsonProperty("expires_at")
    private Instant expiresAt;

    /**
     * @param retentionDays the configured retention, which dates the deletion of an idle session
     */
    public static SavedSessionDTO fromModel(SavedSessionSummary model, int retentionDays) {
        SavedSessionDTO dto = new SavedSessionDTO();
        dto.sessionId = model.getSessionId();
        dto.fileName = model.getFileName();
        dto.profileId = model.getProfileId();
        dto.createdAt = model.getCreatedAt();
        dto.updatedAt = model.getUpdatedAt();
        dto.messageCount = model.getMessageCount();
        dto.expiresAt = model.getUpdatedAt() == null
                ? null
                : model.getUpdatedAt().plus(Duration.ofDays(Math.max(1, retentionDays)));
        return dto;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    @ApiModelProperty(example = "Vitis_2020.xlsx")
    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    @ApiModelProperty(example = "vitis-explorer")
    public String getProfileId() {
        return profileId;
    }

    public void setProfileId(String profileId) {
        this.profileId = profileId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @ApiModelProperty(value = "The last time the conversation changed; it is deleted after the "
            + "configured number of days without activity")
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @ApiModelProperty(value = "Messages exchanged so far")
    public int getMessageCount() {
        return messageCount;
    }

    public void setMessageCount(int messageCount) {
        this.messageCount = messageCount;
    }

    @ApiModelProperty(value = "When the conversation will be deleted if nothing happens in it until then")
    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}

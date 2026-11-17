//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.graphimport.session.store.StoredSessionSummary;

import java.time.Duration;
import java.time.Instant;

/**
 * A stored import, as the list of imports to resume shows it.
 *
 * @author Arnaud Charleroy
 */
public class StoredImportDTO {

    @JsonProperty("session_id")
    private String sessionId;

    @JsonProperty("title")
    private String title;

    @JsonProperty("created_at")
    private Instant createdAt;

    @JsonProperty("updated_at")
    private Instant updatedAt;

    @JsonProperty("expires_at")
    private Instant expiresAt;

    /**
     * @param retentionDays how long an import is kept after its last activity
     */
    public static StoredImportDTO fromModel(StoredSessionSummary summary, int retentionDays) {
        StoredImportDTO dto = new StoredImportDTO();
        dto.sessionId = summary.id();
        dto.title = summary.title();
        dto.createdAt = summary.createdAt();
        dto.updatedAt = summary.updatedAt();
        dto.expiresAt = summary.updatedAt() == null
                ? null
                : summary.updatedAt().plus(Duration.ofDays(Math.max(1, retentionDays)));
        return dto;
    }

    @ApiModelProperty(value = "Import identifier", required = true)
    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    @ApiModelProperty(value = "Name of the import's first file", required = true)
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @ApiModelProperty(value = "When the import was opened")
    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @ApiModelProperty(value = "When the import was last changed")
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @ApiModelProperty(value = "When the import will be deleted if nothing changes it before")
    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}

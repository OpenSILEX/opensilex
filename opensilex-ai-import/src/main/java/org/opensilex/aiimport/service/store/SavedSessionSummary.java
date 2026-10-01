//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.service.store;

import java.time.Instant;

/**
 * One stored conversation as the resumption list shows it: enough to recognise it, without loading
 * its content.
 *
 * @author Arnaud Charleroy
 */
public class SavedSessionSummary {

    private final String sessionId;
    private final String fileName;
    private final String profileId;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final int messageCount;

    public SavedSessionSummary(String sessionId, String fileName, String profileId, Instant createdAt,
                               Instant updatedAt, int messageCount) {
        this.sessionId = sessionId;
        this.fileName = fileName;
        this.profileId = profileId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.messageCount = messageCount;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getFileName() {
        return fileName;
    }

    public String getProfileId() {
        return profileId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public int getMessageCount() {
        return messageCount;
    }
}

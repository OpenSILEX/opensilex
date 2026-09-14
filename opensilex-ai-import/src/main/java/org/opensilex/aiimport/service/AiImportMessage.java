//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * One turn of the conversation as the user sees it.
 * <p>
 * Distinct from {@link org.opensilex.aiimport.service.dto.ChatMessage}, which is the wire format
 * and also carries the system prompt and the raw tool exchanges. What is shown is the user's
 * questions, the assistant's answers, and a trace of the lookups it performed, so that a claim
 * about the instance can be traced back to a query.
 *
 * @author Arnaud Charleroy
 */
public class AiImportMessage {

    public static final String ROLE_USER = "user";
    public static final String ROLE_ASSISTANT = "assistant";

    private String role;
    private String content;

    /**
     * Set when the message is the module's own rather than the assistant's — chiefly the notice
     * that the model could not be reached. The interface translates it; {@link #content} carries
     * the same sentence in English as a fallback.
     */
    private String contentKey;
    private Instant createdAt = Instant.now();

    /**
     * Lookups performed while producing this answer, as short human-readable lines.
     */
    private List<String> lookups = new ArrayList<>();

    /**
     * The draft produced during this turn, if any. The interface renders its card under this
     * message, so a confirmation always sits next to the words that explain it.
     */
    private String proposalId;

    public AiImportMessage() {
    }

    public AiImportMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public static AiImportMessage user(String content) {
        return new AiImportMessage(ROLE_USER, content);
    }

    public String getContentKey() {
        return contentKey;
    }

    public AiImportMessage setContentKey(String contentKey) {
        this.contentKey = contentKey;
        return this;
    }

    public static AiImportMessage assistant(String content) {
        return new AiImportMessage(ROLE_ASSISTANT, content);
    }

    public String getRole() {
        return role;
    }

    public AiImportMessage setRole(String role) {
        this.role = role;
        return this;
    }

    public String getContent() {
        return content;
    }

    public AiImportMessage setContent(String content) {
        this.content = content;
        return this;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public AiImportMessage setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public String getProposalId() {
        return proposalId;
    }

    public AiImportMessage setProposalId(String proposalId) {
        this.proposalId = proposalId;
        return this;
    }

    public List<String> getLookups() {
        return lookups;
    }

    public AiImportMessage setLookups(List<String> lookups) {
        this.lookups = lookups;
        return this;
    }
}

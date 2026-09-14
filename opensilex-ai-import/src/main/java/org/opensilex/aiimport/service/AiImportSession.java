//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.service;

import org.opensilex.aiimport.create.CreationProposal;
import org.opensilex.aiimport.mapping.ColumnMapping;
import org.opensilex.aiimport.profile.DataPoint;
import org.opensilex.aiimport.profile.EventCandidate;
import org.opensilex.aiimport.profile.ExtractedImportPlan;
import org.opensilex.aiimport.resolve.ResolutionReport;
import org.opensilex.aiimport.service.dto.ChatMessage;
import org.opensilex.aiimport.workbook.WorkbookStructure;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * One conversation about one uploaded file, held in memory for the lifetime configured on the
 * module.
 * <p>
 * The uploaded file itself is not kept: it is read once, and only the structure survives. Nothing
 * here is persisted, which is deliberate for a first version that writes nothing.
 *
 * @author Arnaud Charleroy
 */
public class AiImportSession {

    private final String id;

    /**
     * The account that uploaded the file. Any other account is refused access to the session.
     */
    private final URI accountUri;

    private final Instant createdAt = Instant.now();

    private String fileName;
    private String profileId;
    private WorkbookStructure workbook;
    private ExtractedImportPlan plan;
    private ResolutionReport report;
    private List<ColumnMapping> mappings = new ArrayList<>();

    /**
     * The observations read from the file, still in the file's own words. Kept so that inserting
     * the data does not mean reading the workbook again.
     */
    private List<EventCandidate> events = new ArrayList<>();

    private List<DataPoint> dataPoints = new ArrayList<>();

    /**
     * The wire conversation, system prompt and tool exchanges included, resent on every request.
     */
    private final List<ChatMessage> history = new ArrayList<>();

    /**
     * The conversation as shown to the user.
     */
    private final List<AiImportMessage> transcript = new ArrayList<>();

    /**
     * What this conversation has cost so far, as the endpoint reported it.
     */
    private final TokenUsage tokenUsage = new TokenUsage();

    /**
     * The draft awaiting the user's decision, if any. One at a time: a conversation that offered
     * two competing drafts at once would leave nobody sure which button did what.
     */
    private CreationProposal pendingProposal;

    /**
     * Every proposal made in this conversation, applied ones included, keyed by identifier. Kept so
     * that confirming a draft can refuse one that was already applied.
     */
    private final Map<String, CreationProposal> proposals = new LinkedHashMap<>();

    public AiImportSession(String id, URI accountUri) {
        this.id = id;
        this.accountUri = accountUri;
    }

    public String getId() {
        return id;
    }

    public URI getAccountUri() {
        return accountUri;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getFileName() {
        return fileName;
    }

    public AiImportSession setFileName(String fileName) {
        this.fileName = fileName;
        return this;
    }

    public String getProfileId() {
        return profileId;
    }

    public AiImportSession setProfileId(String profileId) {
        this.profileId = profileId;
        return this;
    }

    public WorkbookStructure getWorkbook() {
        return workbook;
    }

    public AiImportSession setWorkbook(WorkbookStructure workbook) {
        this.workbook = workbook;
        return this;
    }

    public ExtractedImportPlan getPlan() {
        return plan;
    }

    public AiImportSession setPlan(ExtractedImportPlan plan) {
        this.plan = plan;
        return this;
    }

    public ResolutionReport getReport() {
        return report;
    }

    public AiImportSession setReport(ResolutionReport report) {
        this.report = report;
        return this;
    }

    public List<ColumnMapping> getMappings() {
        return mappings;
    }

    public AiImportSession setMappings(List<ColumnMapping> mappings) {
        this.mappings = mappings;
        return this;
    }

    public List<EventCandidate> getEvents() {
        return events;
    }

    public AiImportSession setEvents(List<EventCandidate> events) {
        this.events = events;
        return this;
    }

    public List<DataPoint> getDataPoints() {
        return dataPoints;
    }

    public AiImportSession setDataPoints(List<DataPoint> dataPoints) {
        this.dataPoints = dataPoints;
        return this;
    }

    public List<ChatMessage> getHistory() {
        return history;
    }

    public List<AiImportMessage> getTranscript() {
        return transcript;
    }

    public TokenUsage getTokenUsage() {
        return tokenUsage;
    }

    public CreationProposal getPendingProposal() {
        return pendingProposal;
    }

    public AiImportSession setPendingProposal(CreationProposal proposal) {
        this.pendingProposal = proposal;
        if (proposal != null) {
            proposals.put(proposal.getId(), proposal);
        }
        return this;
    }

    public Optional<CreationProposal> getProposal(String id) {
        return Optional.ofNullable(id == null ? null : proposals.get(id));
    }

    public boolean isOwnedBy(URI candidate) {
        return accountUri != null && accountUri.equals(candidate);
    }

    /**
     * Replaces the system prompt, keeping the rest of the conversation. Called after a
     * revalidation, so the assistant argues from the current state of the instance rather than from
     * the state at upload time.
     */
    public void replaceSystemPrompt(String prompt) {
        history.removeIf(message -> ChatMessage.ROLE_SYSTEM.equals(message.getRole()));
        history.add(0, ChatMessage.system(prompt));
    }
}

//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.service.store;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.opensilex.aiimport.create.CreationProposal;
import org.opensilex.aiimport.create.CreationTarget;
import org.opensilex.aiimport.create.objects.ObjectSheetPlan;
import org.opensilex.aiimport.resolve.ConfirmedMatches;
import org.opensilex.aiimport.resolve.ReportCategory;
import org.opensilex.aiimport.resolve.ResourceReference;
import org.opensilex.aiimport.service.AiImportMessage;
import org.opensilex.aiimport.service.AiImportSession;
import org.opensilex.aiimport.service.dto.ChatMessage;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What is kept of a session to resume it: what cannot be recomputed.
 * <p>
 * The conversation, the drafts and the confirmations are the user's work and are stored. The
 * workbook, the plan, the report and the column mapping are not: they are recomputed from the file
 * on resumption, against the instance as it is then — a report stored for a month would describe an
 * instance that no longer exists.
 *
 * <p>
 * Serialised by its fields, not its accessors: the snapshot must hold everything it declares, and
 * a field forgotten by a getter would be lost without a sound.
 *
 * @author Arnaud Charleroy
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class SavedSession {

    @JsonProperty("session_id")
    private String sessionId;

    @JsonProperty("file_name")
    private String fileName;

    @JsonProperty("profile_id")
    private String profileId;

    @JsonProperty("created_at")
    private Instant createdAt;

    /**
     * The conversation as the language model sees it, tool calls and results included: without
     * them, the model's next turn would answer a conversation with holes in it.
     */
    private List<ChatMessage> history = new ArrayList<>();

    /**
     * The conversation as the user sees it.
     */
    private List<AiImportMessage> transcript = new ArrayList<>();

    @JsonProperty("confirmed_matches")
    private List<SavedMatch> confirmedMatches = new ArrayList<>();

    private List<SavedProposal> proposals = new ArrayList<>();

    /**
     * What the user chose for each object sheet: without it, resuming a conversation would ask
     * again for every type and every column.
     */
    @JsonProperty("object_plans")
    private List<SavedObjectPlan> objectPlans = new ArrayList<>();

    @JsonProperty("pending_proposal_id")
    private String pendingProposalId;

    @JsonProperty("prompt_tokens")
    private long promptTokens;

    @JsonProperty("completion_tokens")
    private long completionTokens;

    @JsonProperty("llm_calls")
    private int llmCalls;

    public static SavedSession of(AiImportSession session) {
        SavedSession saved = new SavedSession();
        saved.sessionId = session.getId();
        saved.fileName = session.getFileName();
        saved.profileId = session.getProfileId();
        saved.createdAt = session.getCreatedAt();
        saved.history = new ArrayList<>(session.getHistory());
        saved.transcript = new ArrayList<>(session.getTranscript());
        for (ConfirmedMatches.Entry entry : session.getConfirmedMatches().entries()) {
            saved.confirmedMatches.add(SavedMatch.of(entry));
        }
        for (CreationProposal proposal : session.getProposals()) {
            saved.proposals.add(SavedProposal.of(proposal));
        }
        for (ObjectSheetPlan plan : session.getObjectPlans().values()) {
            saved.objectPlans.add(SavedObjectPlan.of(plan));
        }
        saved.pendingProposalId = session.getPendingProposal() == null
                ? null
                : session.getPendingProposal().getId();
        saved.promptTokens = session.getTokenUsage().getPromptTokens();
        saved.completionTokens = session.getTokenUsage().getCompletionTokens();
        saved.llmCalls = session.getTokenUsage().getCalls();
        return saved;
    }

    /**
     * A session carrying the stored work, before its file is read again. The confirmations are put
     * back first, so that the resolution run on resumption honours them.
     */
    public AiImportSession toSession(URI owner) {
        AiImportSession session = new AiImportSession(sessionId, owner, createdAt);
        session.setFileName(fileName).setProfileId(profileId);
        session.getHistory().addAll(history);
        session.getTranscript().addAll(transcript);
        for (SavedMatch match : confirmedMatches) {
            ReportCategory.fromKey(match.category).ifPresent(category -> session.getConfirmedMatches()
                    .confirm(category, match.fileValue, new ResourceReference(match.uri, match.name)));
        }
        for (SavedProposal saved : proposals) {
            CreationProposal proposal = saved.toProposal();
            if (proposal == null) {
                continue;
            }
            session.restoreProposal(proposal);
            if (proposal.getId().equals(pendingProposalId)) {
                session.setPendingProposal(proposal);
            }
        }
        for (SavedObjectPlan saved : objectPlans) {
            ObjectSheetPlan plan = saved.toPlan();
            session.getObjectPlans().put(plan.getSheet(), plan);
        }
        session.getTokenUsage().restore(promptTokens, completionTokens, llmCalls);
        return session;
    }

    //#region accessors

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

    public List<ChatMessage> getHistory() {
        return history;
    }

    public List<AiImportMessage> getTranscript() {
        return transcript;
    }

    public List<SavedMatch> getConfirmedMatches() {
        return confirmedMatches;
    }

    public List<SavedProposal> getProposals() {
        return proposals;
    }

    public String getPendingProposalId() {
        return pendingProposalId;
    }

    public List<SavedObjectPlan> getObjectPlans() {
        return objectPlans;
    }

    //#endregion

    /**
     * One name of the file the user said means one resource.
     */
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY,
            getterVisibility = JsonAutoDetect.Visibility.NONE,
            isGetterVisibility = JsonAutoDetect.Visibility.NONE)
    public static class SavedMatch {

        private String category;

        @JsonProperty("file_value")
        private String fileValue;

        private URI uri;

        private String name;

        static SavedMatch of(ConfirmedMatches.Entry entry) {
            SavedMatch match = new SavedMatch();
            match.category = entry.getCategory().getKey();
            match.fileValue = entry.getFileValue();
            match.uri = entry.getResource().getUri();
            match.name = entry.getResource().getName();
            return match;
        }

        public String getCategory() {
            return category;
        }

        public String getFileValue() {
            return fileValue;
        }

        public URI getUri() {
            return uri;
        }

        public String getName() {
            return name;
        }
    }

    /**
     * What the user chose for one object sheet.
     */
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY,
            getterVisibility = JsonAutoDetect.Visibility.NONE,
            isGetterVisibility = JsonAutoDetect.Visibility.NONE)
    public static class SavedObjectPlan {

        private String sheet;
        private URI type;
        private boolean included = true;
        private Map<String, String> mapping = new LinkedHashMap<>();

        static SavedObjectPlan of(ObjectSheetPlan plan) {
            SavedObjectPlan saved = new SavedObjectPlan();
            saved.sheet = plan.getSheet();
            saved.type = plan.getType();
            saved.included = plan.isIncluded();
            saved.mapping = new LinkedHashMap<>(plan.getMapping());
            return saved;
        }

        ObjectSheetPlan toPlan() {
            ObjectSheetPlan plan = new ObjectSheetPlan(sheet).setType(type).setIncluded(included);
            if (mapping != null) {
                mapping.forEach(plan::map);
            }
            return plan;
        }

        public String getSheet() {
            return sheet;
        }
    }

    /**
     * A draft, applied or not: an applied one still refuses a second confirmation after resumption.
     */
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY,
            getterVisibility = JsonAutoDetect.Visibility.NONE,
            isGetterVisibility = JsonAutoDetect.Visibility.NONE)
    public static class SavedProposal {

        private String id;
        private String target;

        @JsonProperty("created_at")
        private Instant createdAt;

        private Map<String, String> fields = new LinkedHashMap<>();

        @JsonProperty("field_sources")
        private Map<String, String> fieldSources = new LinkedHashMap<>();

        @JsonProperty("missing_required")
        private List<String> missingRequired = new ArrayList<>();

        private List<String> blockers = new ArrayList<>();
        private String rationale;
        private String status;

        @JsonProperty("result_uri")
        private URI resultUri;

        @JsonProperty("inserted_count")
        private Integer insertedCount;

        static SavedProposal of(CreationProposal proposal) {
            SavedProposal saved = new SavedProposal();
            saved.id = proposal.getId();
            saved.target = proposal.getTarget().name();
            saved.createdAt = proposal.getCreatedAt();
            saved.fields.putAll(proposal.getFields());
            proposal.getFieldSources().forEach((field, source) -> saved.fieldSources.put(field, source.name()));
            saved.missingRequired.addAll(proposal.getMissingRequired());
            saved.blockers.addAll(proposal.getBlockers());
            saved.rationale = proposal.getRationale();
            saved.status = proposal.getStatus().name();
            saved.resultUri = proposal.getResultUri();
            saved.insertedCount = proposal.getInsertedCount();
            return saved;
        }

        /**
         * @return the draft, or null when its target no longer exists in this version of the module
         */
        CreationProposal toProposal() {
            CreationTarget parsedTarget = CreationTarget.parse(target).orElse(null);
            if (parsedTarget == null) {
                return null;
            }
            CreationProposal proposal = new CreationProposal(id, parsedTarget, createdAt);
            fields.forEach((field, value) -> proposal.put(field, value, sourceOf(field)));
            proposal.getMissingRequired().addAll(missingRequired);
            proposal.getBlockers().addAll(blockers);
            proposal.setRationale(rationale)
                    .setStatus(parse(CreationProposal.Status.class, status, CreationProposal.Status.PENDING))
                    .setResultUri(resultUri)
                    .setInsertedCount(insertedCount);
            return proposal;
        }

        private CreationProposal.FieldSource sourceOf(String field) {
            return parse(CreationProposal.FieldSource.class, fieldSources.get(field), null);
        }

        private static <E extends Enum<E>> E parse(Class<E> type, String name, E fallback) {
            if (name == null) {
                return fallback;
            }
            try {
                return Enum.valueOf(type, name);
            } catch (IllegalArgumentException e) {
                return fallback;
            }
        }

        public String getId() {
            return id;
        }

        public String getTarget() {
            return target;
        }

        public Map<String, String> getFields() {
            return fields;
        }

        public String getStatus() {
            return status;
        }
    }
}

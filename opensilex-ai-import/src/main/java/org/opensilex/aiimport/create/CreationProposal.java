//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.create;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A creation the assistant has drafted and the user has not yet confirmed.
 * <p>
 * The draft is validated when it is made, but validated again when it is applied: the instance can
 * change between the two, and the second check is the one that decides. The identifier exists to
 * refuse a proposal that was already applied, not to let one skip validation.
 *
 * @author Arnaud Charleroy
 */
public class CreationProposal {

    public enum Status {

        /**
         * Drafted, shown to the user, awaiting their decision.
         */
        PENDING,

        /**
         * Written. A proposal is applied once and never twice.
         */
        APPLIED,

        CANCELLED
    }

    /**
     * Where a value came from. A date read from the file and a date written by the assistant do not
     * deserve the same trust, and the card says which is which.
     */
    public enum FieldSource {

        /**
         * Read from the uploaded file.
         */
        FILE,

        /**
         * Written by the assistant. Never for a required field: those are asked, not invented.
         */
        ASSISTANT,

        /**
         * Typed or corrected by the user.
         */
        USER
    }

    private final String id;
    private final CreationTarget target;
    private final Instant createdAt = Instant.now();

    private final Map<String, String> fields = new LinkedHashMap<>();
    private final Map<String, FieldSource> fieldSources = new LinkedHashMap<>();

    /**
     * Required fields still empty. The card keeps its button inactive while this is not empty.
     */
    private final List<String> missingRequired = new ArrayList<>();

    /**
     * Translation keys for what prevents the creation outright.
     */
    private final List<String> blockers = new ArrayList<>();

    /**
     * Why the assistant proposed this, in its own words. Shown above the fields.
     */
    private String rationale;

    private Status status = Status.PENDING;

    private URI resultUri;
    private Integer insertedCount;

    public CreationProposal(String id, CreationTarget target) {
        this.id = id;
        this.target = target;
    }

    public String getId() {
        return id;
    }

    public CreationTarget getTarget() {
        return target;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Map<String, String> getFields() {
        return fields;
    }

    public Map<String, FieldSource> getFieldSources() {
        return fieldSources;
    }

    public List<String> getMissingRequired() {
        return missingRequired;
    }

    public List<String> getBlockers() {
        return blockers;
    }

    public String getRationale() {
        return rationale;
    }

    public CreationProposal setRationale(String rationale) {
        this.rationale = rationale;
        return this;
    }

    public Status getStatus() {
        return status;
    }

    public CreationProposal setStatus(Status status) {
        this.status = status;
        return this;
    }

    public URI getResultUri() {
        return resultUri;
    }

    public CreationProposal setResultUri(URI resultUri) {
        this.resultUri = resultUri;
        return this;
    }

    public Integer getInsertedCount() {
        return insertedCount;
    }

    public CreationProposal setInsertedCount(Integer insertedCount) {
        this.insertedCount = insertedCount;
        return this;
    }

    public CreationProposal put(String field, String value, FieldSource source) {
        fields.put(field, value);
        fieldSources.put(field, source);
        return this;
    }

    /**
     * @return true when nothing prevents the creation and no required field is empty
     */
    public boolean isReady() {
        return status == Status.PENDING && blockers.isEmpty() && missingRequired.isEmpty();
    }
}

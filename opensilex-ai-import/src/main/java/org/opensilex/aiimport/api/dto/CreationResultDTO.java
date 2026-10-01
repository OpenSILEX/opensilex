//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import java.net.URI;
import java.util.List;
import java.util.ArrayList;

/**
 * What a creation produced, with the report recomputed so the interface reflects the new state
 * without a second call.
 *
 * @author Arnaud Charleroy
 */
public class CreationResultDTO {

    private String target;

    @JsonProperty("proposal_id")
    private String proposalId;

    /**
     * The created resource, for a project or an experiment.
     */
    private URI uri;

    /**
     * How many values were written, for a data insertion.
     */
    @JsonProperty("inserted_count")
    private Integer insertedCount;

    /**
     * Set when the insertion was refused as a whole. Insertion is all or nothing: rather than write
     * the rows that resolve and drop the others, it refuses and says which rows stand in the way.
     */
    private boolean refused;



    /**
     * Set with a refusal: every faulty row, where the user will look for it, with its values.
     */
    private BulkValidationDTO validation;

    /**
     * The platform's batch histories of the data written. Data larger than the platform imports at
     * once goes in several batches: if one is refused after others were written, those are named
     * here, so the user can find them — and delete them — in the data import history.
     */
    private List<URI> batches = new ArrayList<>();

    private ResolutionReportDTO report;

    @ApiModelProperty(value = "The draft that was applied")
    public String getProposalId() {
        return proposalId;
    }

    public CreationResultDTO setProposalId(String proposalId) {
        this.proposalId = proposalId;
        return this;
    }

    @ApiModelProperty(example = "EXPERIMENT")
    public String getTarget() {
        return target;
    }

    public CreationResultDTO setTarget(String target) {
        this.target = target;
        return this;
    }

    public URI getUri() {
        return uri;
    }

    public CreationResultDTO setUri(URI uri) {
        this.uri = uri;
        return this;
    }

    public Integer getInsertedCount() {
        return insertedCount;
    }

    public CreationResultDTO setInsertedCount(Integer insertedCount) {
        this.insertedCount = insertedCount;
        return this;
    }

    @ApiModelProperty(value = "True when nothing was written because some rows could not be resolved")
    public boolean isRefused() {
        return refused;
    }

    public CreationResultDTO setRefused(boolean refused) {
        this.refused = refused;
        return this;
    }


    @ApiModelProperty(value = "With a refusal: the faulty rows and their errors")
    public BulkValidationDTO getValidation() {
        return validation;
    }

    public CreationResultDTO setValidation(BulkValidationDTO validation) {
        this.validation = validation;
        return this;
    }

    @ApiModelProperty(value = "The report, recomputed after the creation")
    public ResolutionReportDTO getReport() {
        return report;
    }

    public CreationResultDTO setReport(ResolutionReportDTO report) {
        this.report = report;
        return this;
    }

    @ApiModelProperty(value = "Batch histories of the data written, when it went through the data import")
    public List<URI> getBatches() {
        return batches;
    }

    public CreationResultDTO setBatches(List<URI> batches) {
        this.batches = batches == null ? new ArrayList<>() : new ArrayList<>(batches);
        return this;
    }
}

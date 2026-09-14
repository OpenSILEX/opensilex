//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

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

    @JsonProperty("unresolved_count")
    private int unresolvedCount;

    /**
     * The first few offending rows, enough to see the pattern without listing thousands.
     */
    private List<UnresolvedRowDTO> unresolved = new ArrayList<>();

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

    @ApiModelProperty(value = "How many rows could not be resolved in all")
    public int getUnresolvedCount() {
        return unresolvedCount;
    }

    public CreationResultDTO setUnresolvedCount(int unresolvedCount) {
        this.unresolvedCount = unresolvedCount;
        return this;
    }

    @ApiModelProperty(value = "A sample of the rows that could not be resolved")
    public List<UnresolvedRowDTO> getUnresolved() {
        return unresolved;
    }

    public CreationResultDTO setUnresolved(List<UnresolvedRowDTO> unresolved) {
        this.unresolved = unresolved;
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
}

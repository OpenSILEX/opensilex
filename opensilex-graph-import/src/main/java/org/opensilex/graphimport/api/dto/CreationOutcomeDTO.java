//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import java.net.URI;
import java.util.List;

/**
 * What checking or running a step gave: the problems the platform would raise, or the resources created,
 * and the plan as it now stands.
 *
 * @author Arnaud Charleroy
 */
public class CreationOutcomeDTO {

    @JsonProperty("problems")
    @ApiModelProperty(value = "What would stop the step; empty when it would go through")
    private List<CreationPlanDTO.CreationNodeDTO> problems;

    @JsonProperty("created")
    @ApiModelProperty(value = "The resources created; empty for a check")
    private List<URI> created;

    @JsonProperty("created_count")
    @ApiModelProperty(value = "How many resources were created: the data created are counted, not listed")
    private int createdCount;

    @JsonProperty("plan")
    @ApiModelProperty(value = "The plan as it now stands")
    private CreationPlanDTO plan;

    public CreationOutcomeDTO(List<CreationPlanDTO.CreationNodeDTO> problems, List<URI> created, CreationPlanDTO plan) {
        this(problems, created, created.size(), plan);
    }

    /**
     * @param created      the resources created, listed; empty for data
     * @param createdCount how many resources were created
     */
    public CreationOutcomeDTO(List<CreationPlanDTO.CreationNodeDTO> problems, List<URI> created, int createdCount,
                              CreationPlanDTO plan) {
        this.problems = problems;
        this.created = created;
        this.createdCount = createdCount;
        this.plan = plan;
    }

    public List<CreationPlanDTO.CreationNodeDTO> getProblems() {
        return problems;
    }

    public List<URI> getCreated() {
        return created;
    }

    public int getCreatedCount() {
        return createdCount;
    }

    public CreationPlanDTO getPlan() {
        return plan;
    }
}

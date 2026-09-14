//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.monitoring.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import java.time.Instant;
import java.util.List;

/**
 * Triple store volumetry.
 *
 * <p>A bean and not a record: the swagger generator introspects properties by bean
 * convention, and a record's accessors would produce an empty model and a broken
 * TypeScript client.</p>
 *
 * @author Arnaud Charleroy
 */
public class TripleStoreStatsDTO {

    @ApiModelProperty(value = "total number of triples")
    @JsonProperty("triple_count")
    private Long tripleCount;

    @ApiModelProperty(value = "number of named graphs")
    @JsonProperty("graph_count")
    private Integer graphCount;

    @ApiModelProperty(value = "per graph counts, empty unless enabled in the configuration")
    @JsonProperty("graphs")
    private List<GraphTripleCountDTO> graphs;

    @ApiModelProperty(value = "when these figures were computed")
    @JsonProperty("computed_at")
    private Instant computedAt;

    @ApiModelProperty(value = "true when served from the cache rather than freshly computed")
    @JsonProperty("cached")
    private boolean cached;

    @ApiModelProperty(value = "why a figure is missing, null otherwise")
    @JsonProperty("message")
    private String message;

    public Long getTripleCount() {
        return tripleCount;
    }

    public void setTripleCount(Long tripleCount) {
        this.tripleCount = tripleCount;
    }

    public Integer getGraphCount() {
        return graphCount;
    }

    public void setGraphCount(Integer graphCount) {
        this.graphCount = graphCount;
    }

    public List<GraphTripleCountDTO> getGraphs() {
        return graphs;
    }

    public void setGraphs(List<GraphTripleCountDTO> graphs) {
        this.graphs = graphs;
    }

    public Instant getComputedAt() {
        return computedAt;
    }

    public void setComputedAt(Instant computedAt) {
        this.computedAt = computedAt;
    }

    public boolean isCached() {
        return cached;
    }

    public void setCached(boolean cached) {
        this.cached = cached;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}

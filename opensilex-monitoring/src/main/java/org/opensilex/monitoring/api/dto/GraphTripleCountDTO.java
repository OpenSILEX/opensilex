//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.monitoring.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

/**
 * Triple count of one named graph.
 *
 * <p>A bean and not a record: the swagger generator introspects properties by bean
 * convention, and a record's accessors would produce an empty model and a broken
 * TypeScript client.</p>
 *
 * @author Arnaud Charleroy
 */
public class GraphTripleCountDTO {

    @ApiModelProperty(value = "graph URI")
    @JsonProperty("graph")
    private String graph;

    @ApiModelProperty(value = "number of triples in the graph")
    @JsonProperty("triple_count")
    private Long tripleCount;

    public String getGraph() {
        return graph;
    }

    public void setGraph(String graph) {
        this.graph = graph;
    }

    public Long getTripleCount() {
        return tripleCount;
    }

    public void setTripleCount(Long tripleCount) {
        this.tripleCount = tripleCount;
    }

}

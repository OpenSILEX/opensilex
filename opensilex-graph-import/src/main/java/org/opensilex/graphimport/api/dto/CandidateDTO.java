//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.graphimport.interpret.Candidate;
import org.opensilex.graphimport.ontology.PropertyView;

import java.net.URI;
import java.util.List;

/**
 * A possible answer: a property for a column, a class for a sheet, or a column for the identifier of a
 * sheet, with its score and its evidence.
 *
 * @author Arnaud Charleroy
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CandidateDTO {

    @JsonProperty("uri")
    @ApiModelProperty(value = "The property or the class; absent for a column")
    private URI uri;

    @JsonProperty("column")
    @ApiModelProperty(value = "The column position, from 0; only for an identifier column", example = "0")
    private Integer column;

    @JsonProperty("label")
    @ApiModelProperty(value = "What the user reads: the label of the property or class, or the column header")
    private String label;

    @JsonProperty("kind")
    @ApiModelProperty(value = "For a property: DATA for a literal, OBJECT for a link")
    private String kind;

    @JsonProperty("range")
    @ApiModelProperty(value = "For a property: its datatype or the class it links to")
    private URI range;

    @JsonProperty("min_values")
    @ApiModelProperty(value = "For a property: how many values a row needs at least", example = "0")
    private Integer minValues;

    @JsonProperty("max_values")
    @ApiModelProperty(value = "For a property: how many values a row may hold at most; absent when unbounded")
    private Integer maxValues;

    @JsonProperty("score")
    @ApiModelProperty(value = "The sum of the evidence weights, between 0 and 1", example = "0.8")
    private double score;

    @JsonProperty("evidence")
    private List<EvidenceDTO> evidence;

    static CandidateDTO ofUri(Candidate<URI> candidate, String label) {
        CandidateDTO dto = scored(candidate);
        dto.uri = candidate.target();
        dto.label = label;
        return dto;
    }

    static CandidateDTO ofProperty(Candidate<URI> candidate, String label, PropertyView property) {
        CandidateDTO dto = ofUri(candidate, label);
        if (property != null) {
            dto.kind = property.kind().name();
            dto.range = property.range();
            dto.minValues = property.cardinality().min();
            dto.maxValues = property.cardinality().max();
        }
        return dto;
    }

    static CandidateDTO ofColumn(Candidate<Integer> candidate, String header) {
        CandidateDTO dto = scored(candidate);
        dto.column = candidate.target();
        dto.label = header;
        return dto;
    }

    private static CandidateDTO scored(Candidate<?> candidate) {
        CandidateDTO dto = new CandidateDTO();
        dto.score = candidate.score();
        dto.evidence = candidate.evidence().stream().map(EvidenceDTO::fromModel).toList();
        return dto;
    }

    public URI getUri() {
        return uri;
    }

    public Integer getColumn() {
        return column;
    }

    public String getLabel() {
        return label;
    }

    public String getKind() {
        return kind;
    }

    public URI getRange() {
        return range;
    }

    public Integer getMinValues() {
        return minValues;
    }

    public Integer getMaxValues() {
        return maxValues;
    }

    public double getScore() {
        return score;
    }

    public List<EvidenceDTO> getEvidence() {
        return evidence;
    }
}

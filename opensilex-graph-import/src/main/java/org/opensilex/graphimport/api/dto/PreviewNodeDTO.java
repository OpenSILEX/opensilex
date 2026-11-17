//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.aiimport.api.dto.ReportMessageDTO;
import org.opensilex.graphimport.preview.NodeComparison;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * A resource of the import, compared with the instance: its values merged, each with the sheet and row it comes from.
 *
 * @author Arnaud Charleroy
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PreviewNodeDTO {

    @JsonProperty("index")
    @ApiModelProperty(value = "Its index in the import", example = "4")
    private int index;

    @JsonProperty("name")
    @ApiModelProperty(value = "What the user calls it: its identifier, or the place of its row")
    private String name;

    @JsonProperty("class_labels")
    @ApiModelProperty(value = "Its classes, in the user's language")
    private String classLabels;

    @JsonProperty("state")
    @ApiModelProperty(value = "CREATED, COMPLETED, UNCHANGED or CONFLICT", example = "COMPLETED")
    private String state;

    @JsonProperty("existing")
    @ApiModelProperty(value = "The existing resource it is")
    private URI existing;

    @JsonProperty("candidates")
    @ApiModelProperty(value = "The existing resources its name designates, when it designates several")
    private List<URI> candidates;

    @JsonProperty("reason")
    @ApiModelProperty(value = "Why it is in conflict as a whole")
    private ReportMessageDTO reason;

    @JsonProperty("rows")
    @ApiModelProperty(value = "The rows it comes from, one cell each")
    private List<CellRefDTO> rows;

    @JsonProperty("values")
    @ApiModelProperty(value = "Its values, by property")
    private List<PreviewValueDTO> values;

    @JsonProperty("missing")
    @ApiModelProperty(value = "The properties it needs and has no value for, the existing resource's included")
    private List<String> missing;

    @JsonProperty("problems")
    @ApiModelProperty(value = "The problems of the file about it")
    private List<GraphProblemDTO> problems;

    @JsonProperty("to_fix")
    @ApiModelProperty(value = "True when the file must be corrected before it could be written")
    private boolean toFix;

    /**
     * @param classLabels    its classes, in the user's language
     * @param propertyLabels the labels of its properties, by property
     */
    public static PreviewNodeDTO fromModel(NodeComparison model, String classLabels, Map<URI, String> propertyLabels) {
        PreviewNodeDTO dto = new PreviewNodeDTO();
        dto.index = model.index();
        dto.name = model.node().getName();
        dto.classLabels = classLabels;
        dto.state = model.state().name();
        dto.existing = model.existing();
        dto.candidates = model.candidates().stream().sorted().toList();
        dto.reason = model.reason() == null ? null : ReportMessageDTO.fromModel(model.reason());
        dto.rows = model.node().getRows().stream().map(CellRefDTO::fromModel).toList();
        dto.values = model.values().stream()
                .map(value -> PreviewValueDTO.fromModel(value, propertyLabels.get(value.property())))
                .toList();
        dto.missing = model.missing();
        dto.problems = model.problems().stream().map(GraphProblemDTO::fromModel).toList();
        dto.toFix = model.toFix();
        return dto;
    }

    public int getIndex() {
        return index;
    }

    public String getName() {
        return name;
    }

    public String getClassLabels() {
        return classLabels;
    }

    public String getState() {
        return state;
    }

    public URI getExisting() {
        return existing;
    }

    public List<URI> getCandidates() {
        return candidates;
    }

    public ReportMessageDTO getReason() {
        return reason;
    }

    public List<CellRefDTO> getRows() {
        return rows;
    }

    public List<PreviewValueDTO> getValues() {
        return values;
    }

    public List<String> getMissing() {
        return missing;
    }

    public List<GraphProblemDTO> getProblems() {
        return problems;
    }

    public boolean isToFix() {
        return toFix;
    }
}

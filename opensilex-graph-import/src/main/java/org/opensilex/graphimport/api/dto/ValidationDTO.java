//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.aiimport.api.dto.ReportMessageDTO;
import org.opensilex.graphimport.graph.GraphLinks;
import org.opensilex.graphimport.graph.ImportGraph;
import org.opensilex.graphimport.interpret.ClassCatalog;
import org.opensilex.graphimport.measure.DataSheetSummary;
import org.opensilex.graphimport.validate.GraphValidation;
import org.opensilex.graphimport.validate.ValidationReport;

import java.util.List;

/**
 * What the graph of an import holds once its rows are merged, and what is wrong with it.
 *
 * @author Arnaud Charleroy
 */
public class ValidationDTO {

    @JsonProperty("session_id")
    private String sessionId;

    @JsonProperty("node_count")
    @ApiModelProperty(value = "How many resources the import describes, rows sharing an identifier counted once",
            example = "120")
    private int nodeCount;

    @JsonProperty("value_count")
    @ApiModelProperty(value = "How many values, links included", example = "840")
    private int valueCount;

    @JsonProperty("link_count")
    @ApiModelProperty(value = "How many of them are links", example = "240")
    private int linkCount;

    @JsonProperty("total")
    @ApiModelProperty(value = "How many problems there are; the list holds at most " + GraphValidation.MAX_PROBLEMS,
            example = "3")
    private int total;

    @JsonProperty("counts")
    @ApiModelProperty(value = "The problems by sheet, column and kind, all of them counted")
    private List<ProblemCountDTO> counts;

    @JsonProperty("problems")
    private List<GraphProblemDTO> problems;

    @JsonProperty("notes")
    @ApiModelProperty(value = "What the graph left out, and why")
    private List<ReportMessageDTO> notes;

    @JsonProperty("date_columns")
    @ApiModelProperty(value = "The columns of dates, with how they are read")
    private List<DateColumnDTO> dateColumns;

    @JsonProperty("column_links")
    @ApiModelProperty(value = "Where the link cells of each column point, counted")
    private List<ColumnLinkDTO> columnLinks;

    @JsonProperty("shared_resources")
    @ApiModelProperty(value = "The pairs of sheets whose rows are the same resources, counted")
    private List<SharedResourcesDTO> sharedResources;

    /**
     * @param links   where the links land and which sheets share resources
     * @param catalog names the classes the links point to
     */
    @JsonProperty("data_sheets")
    @ApiModelProperty(value = "What each sheet of data gives, counted")
    private List<DataSheetDTO> dataSheets = List.of();

    public static ValidationDTO fromModel(String sessionId, ValidationReport report, ImportGraph graph,
                                          GraphLinks links, ClassCatalog catalog, String language) {
        ValidationDTO dto = new ValidationDTO();
        dto.sessionId = sessionId;
        dto.nodeCount = report.nodeCount();
        dto.valueCount = report.valueCount();
        dto.linkCount = report.linkCount();
        dto.total = report.total();
        dto.counts = report.counts().stream().map(ProblemCountDTO::fromModel).toList();
        dto.problems = report.problems().stream().map(GraphProblemDTO::fromModel).toList();
        dto.notes = ReportMessageDTO.fromModels(graph.notes());
        dto.dateColumns = graph.dateColumns().stream().map(DateColumnDTO::fromModel).toList();
        dto.columnLinks = links.columnLinks().stream()
                .map(link -> ColumnLinkDTO.fromModel(link,
                        link.toClass() == null ? null : catalog.labelOf(link.toClass(), language)))
                .toList();
        dto.sharedResources = links.shared().stream().map(SharedResourcesDTO::fromModel).toList();
        return dto;
    }

    /** Says what each sheet of data gives. */
    public ValidationDTO withData(List<DataSheetSummary> sheets) {
        dataSheets = sheets.stream().map(DataSheetDTO::fromModel).toList();
        return this;
    }

    public List<DataSheetDTO> getDataSheets() {
        return dataSheets;
    }

    public String getSessionId() {
        return sessionId;
    }

    public int getNodeCount() {
        return nodeCount;
    }

    public int getValueCount() {
        return valueCount;
    }

    public int getLinkCount() {
        return linkCount;
    }

    public int getTotal() {
        return total;
    }

    public List<ProblemCountDTO> getCounts() {
        return counts;
    }

    public List<GraphProblemDTO> getProblems() {
        return problems;
    }

    public List<ReportMessageDTO> getNotes() {
        return notes;
    }

    public List<ColumnLinkDTO> getColumnLinks() {
        return columnLinks;
    }

    public List<SharedResourcesDTO> getSharedResources() {
        return sharedResources;
    }

    public List<DateColumnDTO> getDateColumns() {
        return dateColumns;
    }
}

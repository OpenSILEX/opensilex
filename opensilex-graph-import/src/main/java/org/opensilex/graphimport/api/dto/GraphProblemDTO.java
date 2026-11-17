//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.aiimport.api.dto.ReportMessageDTO;
import org.opensilex.graphimport.validate.GraphProblem;

import java.util.List;

/**
 * Something the user has to look at, on the cells it concerns.
 *
 * @author Arnaud Charleroy
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GraphProblemDTO {

    @JsonProperty("severity")
    @ApiModelProperty(value = "ERROR stops the import, WARNING asks the user to check", example = "ERROR")
    private String severity;

    @JsonProperty("message")
    private ReportMessageDTO message;

    @JsonProperty("cells")
    @ApiModelProperty(value = "The cells concerned, the first one first shown")
    private List<CellRefDTO> cells;

    @JsonProperty("about_row")
    @ApiModelProperty(value = "True when it concerns the resource as a whole rather than the cells of one column")
    private boolean aboutRow;

    @JsonProperty("node_name")
    @ApiModelProperty(value = "What the user calls the resource: its identifier, or the place of its row")
    private String nodeName;

    @JsonProperty("value")
    @ApiModelProperty(value = "The cell as written, for a problem about one value")
    private String value;

    @JsonProperty("suggestions")
    @ApiModelProperty(value = "What the value was probably meant to be, closest first")
    private List<SuggestionDTO> suggestions;

    static GraphProblemDTO fromModel(GraphProblem model) {
        GraphProblemDTO dto = new GraphProblemDTO();
        dto.severity = model.severity().name();
        dto.message = ReportMessageDTO.fromModel(model.message());
        dto.cells = model.cells().stream().map(CellRefDTO::fromModel).toList();
        dto.aboutRow = model.aboutRow();
        dto.nodeName = model.nodeName();
        dto.value = model.value();
        dto.suggestions = model.suggestions().stream().map(SuggestionDTO::fromModel).toList();
        return dto;
    }

    public String getSeverity() {
        return severity;
    }

    public ReportMessageDTO getMessage() {
        return message;
    }

    public List<CellRefDTO> getCells() {
        return cells;
    }

    public boolean isAboutRow() {
        return aboutRow;
    }

    public String getNodeName() {
        return nodeName;
    }

    public String getValue() {
        return value;
    }

    public List<SuggestionDTO> getSuggestions() {
        return suggestions;
    }
}

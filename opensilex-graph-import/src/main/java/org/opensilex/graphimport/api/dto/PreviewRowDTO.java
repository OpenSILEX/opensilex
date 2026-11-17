//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.graphimport.preview.PreviewGrid;

import java.util.List;

/**
 * A row of the grid, with the resource it is.
 *
 * @author Arnaud Charleroy
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PreviewRowDTO {

    @JsonProperty("row")
    @ApiModelProperty(value = "Its place among the rows of values, from 0; not the spreadsheet's row number", example = "0")
    private int row;

    @JsonProperty("node")
    @ApiModelProperty(value = "The index of the resource it is; absent for a sheet the graph leaves out", example = "4")
    private Integer node;

    @JsonProperty("node_name")
    @ApiModelProperty(value = "What the user calls the resource")
    private String nodeName;

    @JsonProperty("node_state")
    @ApiModelProperty(value = "CREATED, COMPLETED, UNCHANGED or CONFLICT", example = "CREATED")
    private String nodeState;

    @JsonProperty("to_fix")
    @ApiModelProperty(value = "True when the file must be corrected for the resource")
    private boolean toFix;

    @JsonProperty("cells")
    @ApiModelProperty(value = "Its filled cells")
    private List<PreviewCellDTO> cells;

    static PreviewRowDTO fromModel(PreviewGrid.Row model) {
        PreviewRowDTO dto = new PreviewRowDTO();
        dto.row = model.row();
        if (model.node() != null) {
            dto.node = model.node().index();
            dto.nodeName = model.node().node().getName();
            dto.nodeState = model.node().state().name();
            dto.toFix = model.node().toFix();
        }
        dto.cells = model.cells().stream().map(PreviewCellDTO::fromModel).toList();
        return dto;
    }

    public int getRow() {
        return row;
    }

    public Integer getNode() {
        return node;
    }

    public String getNodeName() {
        return nodeName;
    }

    public String getNodeState() {
        return nodeState;
    }

    public boolean isToFix() {
        return toFix;
    }

    public List<PreviewCellDTO> getCells() {
        return cells;
    }
}

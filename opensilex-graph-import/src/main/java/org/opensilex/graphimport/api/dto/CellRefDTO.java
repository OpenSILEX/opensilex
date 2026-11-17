//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.graphimport.graph.CellRef;

/**
 * A cell of the import.
 *
 * @author Arnaud Charleroy
 */
public class CellRefDTO {

    @JsonProperty("sheet")
    @ApiModelProperty(value = "The sheet, as the import names it")
    private String sheet;

    @JsonProperty("row")
    @ApiModelProperty(value = "The row among the rows of values of the sheet, from 0; not the spreadsheet's row number, "
            + "since empty rows are skipped", example = "0")
    private int row;

    @JsonProperty("column")
    @ApiModelProperty(value = "The column, from 0", example = "0")
    private int column;

    static CellRefDTO fromModel(CellRef model) {
        CellRefDTO dto = new CellRefDTO();
        dto.sheet = model.sheet();
        dto.row = model.row();
        dto.column = model.column();
        return dto;
    }

    public String getSheet() {
        return sheet;
    }

    public int getRow() {
        return row;
    }

    public int getColumn() {
        return column;
    }
}

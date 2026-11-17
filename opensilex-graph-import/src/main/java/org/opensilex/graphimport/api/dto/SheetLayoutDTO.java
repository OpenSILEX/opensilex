//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.graphimport.workbook.SheetLayout;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * Where the header of a sheet is and where its values start, as the spreadsheet numbers its rows:
 * from 1. When it is sent to choose a layout, it also names the import and the sheet.
 *
 * @author Arnaud Charleroy
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SheetLayoutDTO {

    @JsonProperty("session_id")
    @ApiModelProperty(value = "The import; only when choosing a layout")
    private String sessionId;

    @JsonProperty("sheet")
    @ApiModelProperty(value = "The sheet, as the import names it; only when choosing a layout")
    private String sheet;

    @NotNull
    @Min(1)
    @JsonProperty("header_row")
    @ApiModelProperty(value = "The row holding the column headers, from 1", example = "3", required = true)
    private Integer headerRow;

    @NotNull
    @Min(1)
    @JsonProperty("first_data_row")
    @ApiModelProperty(value = "The first row holding values, from 1; rows between the header and it are skipped",
            example = "5", required = true)
    private Integer firstDataRow;

    public static SheetLayoutDTO fromModel(SheetLayout layout) {
        if (layout == null) {
            return null;
        }
        SheetLayoutDTO dto = new SheetLayoutDTO();
        dto.headerRow = layout.headerRow() + 1;
        dto.firstDataRow = layout.firstDataRow() + 1;
        return dto;
    }

    public SheetLayout toModel() {
        return new SheetLayout(headerRow - 1, firstDataRow - 1);
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSheet() {
        return sheet;
    }

    public void setSheet(String sheet) {
        this.sheet = sheet;
    }

    public Integer getHeaderRow() {
        return headerRow;
    }

    public void setHeaderRow(Integer headerRow) {
        this.headerRow = headerRow;
    }

    public Integer getFirstDataRow() {
        return firstDataRow;
    }

    public void setFirstDataRow(Integer firstDataRow) {
        this.firstDataRow = firstDataRow;
    }
}

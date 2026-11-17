//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.graphimport.session.SheetLayouts;

import java.util.List;

/**
 * The first rows of a sheet as the file holds them, to choose its header row and its first row of
 * values from.
 *
 * @author Arnaud Charleroy
 */
public class SheetRowsDTO {

    @JsonProperty("sheet")
    private String sheet;

    @JsonProperty("file_name")
    @ApiModelProperty(value = "The file the sheet comes from")
    private String fileName;

    @JsonProperty("layout")
    @ApiModelProperty(value = "The layout in use; absent when no table is recognised and none was chosen")
    private SheetLayoutDTO layout;

    @JsonProperty("detected_layout")
    @ApiModelProperty(value = "The layout the reader found; absent when it found no table")
    private SheetLayoutDTO detectedLayout;

    @JsonProperty("rows")
    @ApiModelProperty(value = "The first rows; the first one is row 1 of the spreadsheet")
    private List<List<String>> rows;

    @JsonProperty("total_rows")
    @ApiModelProperty(value = "How many rows the sheet has", example = "120")
    private int totalRows;

    public static SheetRowsDTO fromModel(String sheet, SheetLayouts.SheetRows rows) {
        SheetRowsDTO dto = new SheetRowsDTO();
        dto.sheet = sheet;
        dto.fileName = rows.location().file().getName();
        dto.layout = SheetLayoutDTO.fromModel(rows.current());
        dto.detectedLayout = SheetLayoutDTO.fromModel(rows.detected());
        dto.rows = rows.rows();
        dto.totalRows = rows.total();
        return dto;
    }

    public String getSheet() {
        return sheet;
    }

    public String getFileName() {
        return fileName;
    }

    public SheetLayoutDTO getLayout() {
        return layout;
    }

    public SheetLayoutDTO getDetectedLayout() {
        return detectedLayout;
    }

    public List<List<String>> getRows() {
        return rows;
    }

    public int getTotalRows() {
        return totalRows;
    }
}

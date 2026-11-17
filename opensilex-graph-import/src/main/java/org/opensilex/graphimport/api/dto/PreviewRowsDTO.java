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
 * A page of a sheet as the preview shows it.
 *
 * @author Arnaud Charleroy
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PreviewRowsDTO {

    @JsonProperty("sheet")
    @ApiModelProperty(value = "The sheet, as the import names it")
    private String sheet;

    @JsonProperty("columns")
    @ApiModelProperty(value = "Its columns")
    private List<PreviewColumnDTO> columns;

    @JsonProperty("rows")
    @ApiModelProperty(value = "The rows of the page")
    private List<PreviewRowDTO> rows;

    @JsonProperty("page")
    @ApiModelProperty(value = "The page, from 0", example = "0")
    private int page;

    @JsonProperty("page_size")
    @ApiModelProperty(value = "Rows per page", example = "20")
    private int pageSize;

    @JsonProperty("total_count")
    @ApiModelProperty(value = "How many rows the filter keeps", example = "120")
    private int totalCount;

    public static PreviewRowsDTO fromModel(String sheet, PreviewGrid.Page model, int page, int pageSize) {
        PreviewRowsDTO dto = new PreviewRowsDTO();
        dto.sheet = sheet;
        dto.columns = model.columns().stream().map(PreviewColumnDTO::fromModel).toList();
        dto.rows = model.rows().stream().map(PreviewRowDTO::fromModel).toList();
        dto.page = page;
        dto.pageSize = pageSize;
        dto.totalCount = model.total();
        return dto;
    }

    public String getSheet() {
        return sheet;
    }

    public List<PreviewColumnDTO> getColumns() {
        return columns;
    }

    public List<PreviewRowDTO> getRows() {
        return rows;
    }

    public int getPage() {
        return page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public int getTotalCount() {
        return totalCount;
    }
}

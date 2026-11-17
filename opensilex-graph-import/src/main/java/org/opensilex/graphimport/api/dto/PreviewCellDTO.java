//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.graphimport.preview.PreviewGrid;

/**
 * A filled cell of the grid, with what the import would do with it.
 *
 * @author Arnaud Charleroy
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PreviewCellDTO {

    @JsonProperty("column")
    @ApiModelProperty(value = "Its column, from 0", example = "2")
    private int column;

    @JsonProperty("raw")
    @ApiModelProperty(value = "As written")
    private String raw;

    @JsonProperty("state")
    @ApiModelProperty(value = "NEW, PRESENT, CONFLICT, AMBIGUOUS, ERROR, IDENTIFIER, TYPE or IGNORED", example = "NEW")
    private String state;

    static PreviewCellDTO fromModel(PreviewGrid.Cell model) {
        PreviewCellDTO dto = new PreviewCellDTO();
        dto.column = model.column();
        dto.raw = model.raw();
        dto.state = model.state().name();
        return dto;
    }

    public int getColumn() {
        return column;
    }

    public String getRaw() {
        return raw;
    }

    public String getState() {
        return state;
    }
}

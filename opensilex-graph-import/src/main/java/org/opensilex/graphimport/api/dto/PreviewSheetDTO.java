//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.graphimport.preview.SheetMap;

/**
 * A sheet of the import, on the map of the sheets.
 *
 * @author Arnaud Charleroy
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PreviewSheetDTO {

    @JsonProperty("name")
    @ApiModelProperty(value = "The sheet, as the import names it")
    private String name;

    @JsonProperty("class_label")
    @ApiModelProperty(value = "The class of its rows, in the user's language; absent while not chosen", example = "Plot")
    private String classLabel;

    @JsonProperty("row_count")
    @ApiModelProperty(value = "How many rows of values it has", example = "40")
    private int rowCount;

    @JsonProperty("resources")
    @ApiModelProperty(value = "How many resources its rows are", example = "38")
    private int resources;

    static PreviewSheetDTO fromModel(SheetMap.Sheet model) {
        PreviewSheetDTO dto = new PreviewSheetDTO();
        dto.name = model.name();
        dto.classLabel = model.classLabel();
        dto.rowCount = model.rowCount();
        dto.resources = model.resources();
        return dto;
    }

    public String getName() {
        return name;
    }

    public String getClassLabel() {
        return classLabel;
    }

    public int getRowCount() {
        return rowCount;
    }

    public int getResources() {
        return resources;
    }
}

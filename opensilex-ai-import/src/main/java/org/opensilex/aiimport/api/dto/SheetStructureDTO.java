//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.aiimport.workbook.SheetStructure;

import java.util.List;

/**
 * One sheet of an uploaded file, as shown in the interface.
 *
 * @author Arnaud Charleroy
 */
public class SheetStructureDTO {

    private String name;

    @JsonProperty("is_tabular")
    private boolean tabular;

    private List<String> headers;

    @JsonProperty("data_row_count")
    private int dataRowCount;

    @JsonProperty("sample_rows")
    private List<List<String>> sampleRows;

    private String text;

    public static SheetStructureDTO fromModel(SheetStructure model, int sampleRows) {
        SheetStructureDTO dto = new SheetStructureDTO();
        dto.name = model.getName();
        dto.tabular = model.isTabular();
        dto.text = model.getText();
        if (model.isTabular()) {
            dto.headers = model.getHeaders();
            dto.dataRowCount = model.getDataRowCount();
            dto.sampleRows = model.sample(sampleRows);
        }
        return dto;
    }

    @ApiModelProperty(example = "Cartouche_Fixe")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ApiModelProperty(value = "False for a sheet holding prose rather than a table, such as a ReadMe")
    public boolean isTabular() {
        return tabular;
    }

    public void setTabular(boolean tabular) {
        this.tabular = tabular;
    }

    public List<String> getHeaders() {
        return headers;
    }

    public void setHeaders(List<String> headers) {
        this.headers = headers;
    }

    public int getDataRowCount() {
        return dataRowCount;
    }

    public void setDataRowCount(int dataRowCount) {
        this.dataRowCount = dataRowCount;
    }

    @ApiModelProperty(value = "A bounded sample of the data rows, for display")
    public List<List<String>> getSampleRows() {
        return sampleRows;
    }

    public void setSampleRows(List<List<String>> sampleRows) {
        this.sampleRows = sampleRows;
    }

    @ApiModelProperty(value = "Text of a non-tabular sheet")
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}

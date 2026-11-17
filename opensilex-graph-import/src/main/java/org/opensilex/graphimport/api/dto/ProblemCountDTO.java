//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.aiimport.api.dto.ReportMessageDTO;
import org.opensilex.graphimport.validate.ProblemCount;

/**
 * How many problems of one kind a column, or the rows of a sheet, have.
 *
 * @author Arnaud Charleroy
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProblemCountDTO {

    @JsonProperty("sheet")
    @ApiModelProperty(value = "The sheet, as the import names it")
    private String sheet;

    @JsonProperty("column")
    @ApiModelProperty(value = "The column, from 0; absent for problems about whole rows", example = "2")
    private Integer column;

    @JsonProperty("severity")
    @ApiModelProperty(value = "ERROR or WARNING", example = "ERROR")
    private String severity;

    @JsonProperty("count")
    @ApiModelProperty(value = "How many there are", example = "12")
    private int count;

    @JsonProperty("example")
    @ApiModelProperty(value = "The first of them")
    private ReportMessageDTO example;

    static ProblemCountDTO fromModel(ProblemCount model) {
        ProblemCountDTO dto = new ProblemCountDTO();
        dto.sheet = model.sheet();
        dto.column = model.column();
        dto.severity = model.severity().name();
        dto.count = model.count();
        dto.example = ReportMessageDTO.fromModel(model.example());
        return dto;
    }

    public String getSheet() {
        return sheet;
    }

    public Integer getColumn() {
        return column;
    }

    public String getSeverity() {
        return severity;
    }

    public int getCount() {
        return count;
    }

    public ReportMessageDTO getExample() {
        return example;
    }
}

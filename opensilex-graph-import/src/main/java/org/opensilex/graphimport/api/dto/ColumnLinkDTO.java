//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.graphimport.graph.GraphLinks;

import java.net.URI;

/**
 * Where the link cells of one column point, counted.
 *
 * @author Arnaud Charleroy
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ColumnLinkDTO {

    @JsonProperty("sheet")
    @ApiModelProperty(value = "The sheet of the column")
    private String sheet;

    @JsonProperty("column")
    @ApiModelProperty(value = "The column, from 0", example = "2")
    private int column;

    @JsonProperty("property")
    @ApiModelProperty(value = "The property of the column")
    private URI property;

    @JsonProperty("kind")
    @ApiModelProperty(value = "ROW: rows of the file; INSTANCE: existing resources; UNRESOLVED: nothing settled", example = "ROW")
    private String kind;

    @JsonProperty("to_sheet")
    @ApiModelProperty(value = "For ROW, the first sheet, in the workbook order, where the rows named are")
    private String toSheet;

    @JsonProperty("to_class")
    @ApiModelProperty(value = "For INSTANCE, the range of the property")
    private URI toClass;

    @JsonProperty("to_class_label")
    @ApiModelProperty(value = "For INSTANCE, the range's label in the user's language")
    private String toClassLabel;

    @JsonProperty("count")
    @ApiModelProperty(value = "How many cells", example = "12")
    private int count;

    static ColumnLinkDTO fromModel(GraphLinks.ColumnLink model, String toClassLabel) {
        ColumnLinkDTO dto = new ColumnLinkDTO();
        dto.sheet = model.sheet();
        dto.column = model.column();
        dto.property = model.property();
        dto.kind = model.kind().name();
        dto.toSheet = model.toSheet();
        dto.toClass = model.toClass();
        dto.toClassLabel = toClassLabel;
        dto.count = model.count();
        return dto;
    }

    public String getSheet() {
        return sheet;
    }

    public int getColumn() {
        return column;
    }

    public URI getProperty() {
        return property;
    }

    public String getKind() {
        return kind;
    }

    public String getToSheet() {
        return toSheet;
    }

    public URI getToClass() {
        return toClass;
    }

    public String getToClassLabel() {
        return toClassLabel;
    }

    public int getCount() {
        return count;
    }
}

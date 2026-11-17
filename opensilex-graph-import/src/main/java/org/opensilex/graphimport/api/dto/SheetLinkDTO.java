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
 * How two sheets, or a sheet and existing resources, hang together.
 *
 * @author Arnaud Charleroy
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SheetLinkDTO {

    @JsonProperty("from")
    @ApiModelProperty(value = "The sheet the rows or cells are in")
    private String from;

    @JsonProperty("to")
    @ApiModelProperty(value = "The other sheet; for INSTANCE, the class of the existing resources")
    private String to;

    @JsonProperty("kind")
    @ApiModelProperty(value = "SAME_RESOURCE: rows of both sheets are the same resources; ROW: cells point to rows "
            + "of the other sheet; INSTANCE: cells point to existing resources", example = "ROW")
    private String kind;

    @JsonProperty("property")
    @ApiModelProperty(value = "The label of the property of the links; absent for SAME_RESOURCE")
    private String property;

    @JsonProperty("count")
    @ApiModelProperty(value = "How many resources, or link cells, say so", example = "12")
    private int count;

    static SheetLinkDTO fromModel(SheetMap.Link model) {
        SheetLinkDTO dto = new SheetLinkDTO();
        dto.from = model.from();
        dto.to = model.to();
        dto.kind = model.kind().name();
        dto.property = model.property();
        dto.count = model.count();
        return dto;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public String getKind() {
        return kind;
    }

    public String getProperty() {
        return property;
    }

    public int getCount() {
        return count;
    }
}

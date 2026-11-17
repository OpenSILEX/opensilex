//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.graphimport.graph.GraphLinks;

/**
 * Two sheets whose rows share identifiers, and so describe the same resources.
 *
 * @author Arnaud Charleroy
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SharedResourcesDTO {

    @JsonProperty("from")
    @ApiModelProperty(value = "The sheet that comes first in the workbook")
    private String from;

    @JsonProperty("to")
    @ApiModelProperty(value = "The other sheet")
    private String to;

    @JsonProperty("count")
    @ApiModelProperty(value = "How many resources they share", example = "40")
    private int count;

    static SharedResourcesDTO fromModel(GraphLinks.SharedResources model) {
        SharedResourcesDTO dto = new SharedResourcesDTO();
        dto.from = model.from();
        dto.to = model.to();
        dto.count = model.count();
        return dto;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public int getCount() {
        return count;
    }
}

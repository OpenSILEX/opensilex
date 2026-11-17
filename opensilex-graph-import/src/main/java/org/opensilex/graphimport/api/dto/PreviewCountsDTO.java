//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.graphimport.preview.PreviewCounts;

/**
 * How many resources the import would create, complete, leave as they are, or cannot settle.
 *
 * @author Arnaud Charleroy
 */
public class PreviewCountsDTO {

    @JsonProperty("created")
    @ApiModelProperty(value = "Resources nothing in the instance answers to", example = "12")
    private int created;

    @JsonProperty("completed")
    @ApiModelProperty(value = "Existing resources the file gives new values", example = "3")
    private int completed;

    @JsonProperty("unchanged")
    @ApiModelProperty(value = "Existing resources that hold every value already", example = "5")
    private int unchanged;

    @JsonProperty("conflict")
    @ApiModelProperty(value = "Resources the file and the instance disagree on", example = "1")
    private int conflict;

    @JsonProperty("to_fix")
    @ApiModelProperty(value = "Resources the file must be corrected for, whatever their state", example = "2")
    private int toFix;

    @JsonProperty("total")
    @ApiModelProperty(value = "How many resources", example = "21")
    private int total;

    static PreviewCountsDTO fromModel(PreviewCounts model) {
        PreviewCountsDTO dto = new PreviewCountsDTO();
        dto.created = model.created();
        dto.completed = model.completed();
        dto.unchanged = model.unchanged();
        dto.conflict = model.conflict();
        dto.toFix = model.toFix();
        dto.total = model.total();
        return dto;
    }

    public int getCreated() {
        return created;
    }

    public int getCompleted() {
        return completed;
    }

    public int getUnchanged() {
        return unchanged;
    }

    public int getConflict() {
        return conflict;
    }

    public int getToFix() {
        return toFix;
    }

    public int getTotal() {
        return total;
    }
}

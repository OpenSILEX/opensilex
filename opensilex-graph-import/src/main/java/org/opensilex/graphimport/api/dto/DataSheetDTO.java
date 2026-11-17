//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.graphimport.measure.DataSheetSummary;

/**
 * What a sheet of data gives, counted.
 *
 * @author Arnaud Charleroy
 */
public class DataSheetDTO {

    @JsonProperty("sheet")
    private String sheet;

    @JsonProperty("ready")
    @ApiModelProperty(value = "The data that could be created now")
    private int ready;

    @JsonProperty("waiting")
    @ApiModelProperty(value = "The data whose target is a row of the file still to create")
    private int waiting;

    @JsonProperty("not_available")
    @ApiModelProperty(value = "The cells written as not available (NA): no datum")
    private int notAvailable;

    @JsonProperty("refused")
    @ApiModelProperty(value = "The cells that give no datum because of a problem")
    private int refused;

    public static DataSheetDTO fromModel(DataSheetSummary summary) {
        DataSheetDTO dto = new DataSheetDTO();
        dto.sheet = summary.sheet();
        dto.ready = summary.ready();
        dto.waiting = summary.waiting();
        dto.notAvailable = summary.notAvailable();
        dto.refused = summary.refused();
        return dto;
    }

    public String getSheet() {
        return sheet;
    }

    public int getReady() {
        return ready;
    }

    public int getWaiting() {
        return waiting;
    }

    public int getNotAvailable() {
        return notAvailable;
    }

    public int getRefused() {
        return refused;
    }
}

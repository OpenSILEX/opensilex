//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.graphimport.binding.ColumnReading;
import org.opensilex.graphimport.ontology.literal.DateOrder;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.time.ZoneOffset;

/**
 * How the dates of a column are written, where the cells cannot tell.
 *
 * @author Arnaud Charleroy
 */
public class ColumnReadingDTO {

    @NotNull
    @JsonProperty("session_id")
    @ApiModelProperty(value = "The import", required = true)
    private String sessionId;

    @NotNull
    @JsonProperty("sheet")
    @ApiModelProperty(value = "The sheet, as the import names it", required = true)
    private String sheet;

    @NotNull
    @Min(0)
    @JsonProperty("column")
    @ApiModelProperty(value = "The column, from 0", example = "1", required = true)
    private Integer column;

    @JsonProperty("date_order")
    @ApiModelProperty(value = "The order of day and month; absent to let the values decide", example = "DAY_FIRST")
    private DateOrder dateOrder;

    @Pattern(regexp = "^[+-](?:0\\d|1[0-4]):[0-5]\\d$")
    @JsonProperty("zone")
    @ApiModelProperty(value = "The offset of times written without one; absent to refuse them", example = "+02:00")
    private String zone;

    public ColumnReading toModel() {
        return new ColumnReading(dateOrder, zone == null ? null : ZoneOffset.of(zone));
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSheet() {
        return sheet;
    }

    public void setSheet(String sheet) {
        this.sheet = sheet;
    }

    public Integer getColumn() {
        return column;
    }

    public void setColumn(Integer column) {
        this.column = column;
    }

    public DateOrder getDateOrder() {
        return dateOrder;
    }

    public void setDateOrder(DateOrder dateOrder) {
        this.dateOrder = dateOrder;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }
}

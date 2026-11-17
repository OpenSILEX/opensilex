//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.graphimport.graph.DateColumn;

import java.net.URI;

/**
 * A column of dates, and how its cells are read.
 *
 * @author Arnaud Charleroy
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DateColumnDTO {

    @JsonProperty("sheet")
    @ApiModelProperty(value = "The sheet, as the import names it")
    private String sheet;

    @JsonProperty("column")
    @ApiModelProperty(value = "The column, from 0", example = "1")
    private int column;

    @JsonProperty("header")
    private String header;

    @JsonProperty("datatype")
    @ApiModelProperty(value = "xsd:date or xsd:dateTime")
    private URI datatype;

    @JsonProperty("detected_order")
    @ApiModelProperty(value = "The order of day and month the values show: DAY_FIRST, MONTH_FIRST or UNDECIDED",
            example = "DAY_FIRST")
    private String detectedOrder;

    @JsonProperty("date_order")
    @ApiModelProperty(value = "The order the user chose; absent when the values decide", example = "DAY_FIRST")
    private String dateOrder;

    @JsonProperty("zone")
    @ApiModelProperty(value = "The offset the user chose for times written without one; absent to refuse them",
            example = "+02:00")
    private String zone;

    static DateColumnDTO fromModel(DateColumn model) {
        DateColumnDTO dto = new DateColumnDTO();
        dto.sheet = model.sheet();
        dto.column = model.column();
        dto.header = model.header();
        dto.datatype = model.datatype();
        dto.detectedOrder = model.detected().name();
        dto.dateOrder = model.chosen().dateOrder() == null ? null : model.chosen().dateOrder().name();
        dto.zone = model.chosen().zone() == null ? null : model.chosen().zone().getId();
        return dto;
    }

    public String getSheet() {
        return sheet;
    }

    public int getColumn() {
        return column;
    }

    public String getHeader() {
        return header;
    }

    public URI getDatatype() {
        return datatype;
    }

    public String getDetectedOrder() {
        return detectedOrder;
    }

    public String getDateOrder() {
        return dateOrder;
    }

    public String getZone() {
        return zone;
    }
}

//******************************************************************************
//                          DataCreationDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.data.api;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.core.data.dal.DataComputedModel;
import org.opensilex.core.data.dal.DataModel;
import org.opensilex.core.data.dal.DataSimpleModel;
import org.opensilex.server.rest.validation.DateFormat;
import org.opensilex.server.rest.validation.Required;
import org.opensilex.server.rest.validation.ValidURI;

import javax.validation.constraints.NotNull;
import java.net.URI;
import java.time.*;
import java.time.format.DateTimeFormatter;

/**
 * This class provide a light DTO for data.
 *
 * @author brice maussang
 */
@JsonPropertyOrder({
        "date", "value"
})
public class DataComputedGetDTO {

    @Required
    @ApiModelProperty(value = "date or datetime", example = DataAPI.DATA_EXAMPLE_MINIMAL_DATE, required = true)
    private String date;

    @NotNull
    @ApiModelProperty(value = "can be decimal, integer, boolean, string or date", example = DataAPI.DATA_EXAMPLE_VALUE)
    private Object value;

    private Instant dateTime;


    public String getDate() {
        return date;
    }

    public void setDate(String date) { this.date = date; }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public Instant getDateTime() { return dateTime; }

    public void setDateTime(Instant dateTime) {
        this.dateTime = dateTime;
        this.setDate(this.dateTime.toString());
    }

    /**
     * Retreive a DTO from the given model.
     *
     * @param model the data model
     * @return a simple data DTO
     */
    public static DataComputedGetDTO getDtoFromModel(DataComputedModel model) {
        DataComputedGetDTO dto = new DataComputedGetDTO();

        dto.setDateTime(model.getDate());
        dto.setValue(model.getValue());

        return dto;
    }

    public DataComputedGetDTO() {

    }

    public DataComputedGetDTO(DataComputedGetDTO other) {
        this.date = other.date;
        this.value = other.value;
        this.dateTime = other.dateTime;
    }

}

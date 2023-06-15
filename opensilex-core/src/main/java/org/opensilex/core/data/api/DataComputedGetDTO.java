//******************************************************************************
//                          DataComputedGetDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.data.api;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.core.data.dal.DataComputedModel;
import org.opensilex.core.data.dal.DataModel;
import org.opensilex.server.rest.validation.Required;

import javax.validation.constraints.NotNull;
import java.time.*;

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
    private ZonedDateTime date;

    @NotNull
    @ApiModelProperty(value = "can be decimal, integer, boolean, string or date", example = DataAPI.DATA_EXAMPLE_VALUE)
    private Object value;


    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public ZonedDateTime getDate() { return date; }

    public void setDate(ZonedDateTime date) {
        this.date = date;
    }

    /**
     * Retreive a DTO from the given model.
     *
     * @param model the data model
     * @return a simple data DTO
     */
    public static DataComputedGetDTO getDtoFromModel(DataComputedModel model) {
        DataComputedGetDTO dto = new DataComputedGetDTO();

        dto.setDate(ZonedDateTime.ofInstant(model.getDate(), ZoneId.systemDefault()));
        dto.setValue(model.getValue());

        return dto;
    }

    public static DataComputedGetDTO getDtoFromModel(DataModel model) {
        DataComputedGetDTO dto = new DataComputedGetDTO();

        dto.setDate(ZonedDateTime.ofInstant(model.getDate(), ZoneId.of(model.getOffset())));
        dto.setValue(model.getValue());

        return dto;
    }

    public DataComputedGetDTO() {

    }

    public DataComputedGetDTO(DataComputedGetDTO other) {
        this.date = other.date;
        this.value = other.value;
    }

}

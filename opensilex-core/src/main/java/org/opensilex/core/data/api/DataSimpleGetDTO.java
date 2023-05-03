//******************************************************************************
//                          DataCreationDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.data.api;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.core.data.dal.DataModel;
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
        "uri", "date", "value"
})
public class DataSimpleGetDTO {
    
    @ValidURI
    @ApiModelProperty(example = DataAPI.DATA_EXAMPLE_URI) 
    protected URI uri;
    
    @Required
    @ApiModelProperty(value = "date or datetime", example = DataAPI.DATA_EXAMPLE_MINIMAL_DATE, required = true)
    private String date;

    @NotNull
    @ApiModelProperty(value = "can be decimal, integer, boolean, string or date", example = DataAPI.DATA_EXAMPLE_VALUE)
    private Object value;

    private Instant dateTime;
    private String offset;
    private Boolean isDateTime;


    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

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

    public void setDateTime(Instant dateTime, String offset, Boolean isDateTime) {
        this.dateTime = dateTime;
        this.offset = offset;
        this.isDateTime = isDateTime;
    }

    public void updateDate(Instant dateTime) {
        setDate(dateTime, this.offset, this.isDateTime);
    }

    /**
     * Set date attribute by converting given parameters in a String.
     *
     * @param instant the date as Instant
     * @param offset timezone offset
     * @param isDateTime true if it's a DateTime
     */
    public void setDate(Instant instant, String offset, Boolean isDateTime) {
        setDateTime(instant, offset, isDateTime);
        if (isDateTime) {
            OffsetDateTime odt = instant.atOffset(ZoneOffset.of(offset));
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern(DateFormat.YMDTHMSMSX.toString());
            this.setDate(dtf.format(odt));
        } else {
            LocalDate date = ZonedDateTime.ofInstant(instant, ZoneId.of(offset)).toLocalDate();
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern(DateFormat.YMD.toString());            ;
            this.setDate(dtf.format(date));
        }
    }

    /**
     * Retreive a DTO from the given model.
     *
     * @param model the data model
     * @return a simple data DTO
     */
    public static DataSimpleGetDTO getDtoFromModel(DataModel model) {
        DataSimpleGetDTO dto = new DataSimpleGetDTO();

        dto.setUri(model.getUri());
        dto.setDate(model.getDate(), model.getOffset(), model.getIsDateTime());
        dto.setValue(model.getValue());

        return dto;
    }

    public DataSimpleGetDTO() {

    }

    public DataSimpleGetDTO(DataSimpleGetDTO other) {
        this.uri = other.uri;
        this.date = other.date;
        this.value = other.value;
        this.dateTime = other.dateTime;
        this.offset = other.offset;
        this.isDateTime = other.isDateTime;
    }

}

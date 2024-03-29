//******************************************************************************
//                          DataGetDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.data.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.core.data.dal.DataDAO;
import org.opensilex.core.data.dal.DataModel;
import org.opensilex.security.user.api.UserGetDTO;
import org.opensilex.server.rest.validation.DateFormat;
import org.opensilex.server.rest.validation.ValidURI;
import org.opensilex.utils.ListWithPagination;

import javax.validation.constraints.NotNull;
import java.net.URI;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Set;

/**
 *
 * @author sammy
 */
public class DataGetDTO extends DataCreationDTO {

    @JsonProperty("publisher")
    private UserGetDTO publisher;

    @JsonProperty("issued")
    private Instant publicationDate;

    @JsonProperty("modified")
    private Instant lastUpdatedDate;

    @NotNull
    @ValidURI
    @ApiModelProperty(value = "data URI", example = DataAPI.DATA_EXAMPLE_URI)    
    @Override
    public URI getUri() {
        return uri;
    }        
    
    @JsonIgnore
    @Override
    public String getTimezone() {
        return timezone;
    }

    public UserGetDTO getPublisher() {
        return publisher;
    }

    public void setPublisher(UserGetDTO publisher) {
        this.publisher = publisher;
    }

    public Instant getPublicationDate() {
        return publicationDate;
    }

    public void setPublicationDate(Instant publicationDate) {
        this.publicationDate = publicationDate;
    }

    public Instant getLastUpdatedDate() {
        return lastUpdatedDate;
    }

    public void setLastUpdatedDate(Instant lastUpdatedDate) {
        this.lastUpdatedDate = lastUpdatedDate;
    }

    public void setDate(Instant instant, String offset, Boolean isDateTime) {
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
     * <p>
     *     Sets the fields of the DataGetDTO according to the passed {@link DataModel}.
     * </p>
     * <p>
     *     If the dateVariables argument is set, the method will check if it contains the variable associated with the
     *     data. If this is the case, the value will be assumed to be a {@link ZonedDateTime} and will be converted to
     *     a {@link LocalDate} to be displayed correctly.
     * </p>
     * <p>
     *     The preferred way of performing this operation is by using {@link DataDAO#modelToDTO(DataModel)},
     *     or {@link DataDAO#modelListToDTO(ListWithPagination)} in the case of a list.
     * </p>
     *
     * @param model
     * @param dateVariables
     */
    public void fromModel(DataModel model, Set<URI> dateVariables) {
        setUri(model.getUri());
        setTarget(model.getTarget());
        setVariable(model.getVariable());      
        setDate(model.getDate(), model.getOffset(), model.getIsDateTime());          
        setConfidence(model.getConfidence());

        if (Objects.nonNull(dateVariables) && dateVariables.contains(getVariable())) {
            setValue(toLocalDate(model.getValue()));
        } else {
            setValue(model.getValue());
        }

        if (Objects.nonNull(model.getPublicationDate())) {
            setPublicationDate(model.getPublicationDate());
        }
        if (Objects.nonNull(model.getLastUpdateDate())) {
            setLastUpdatedDate(model.getLastUpdateDate());
        }

        setMetadata(model.getMetadata());   
        setProvenance(model.getProvenance());
        setRawData(model.getRawData());
    }

    /**
     * <p>
     *     Creates a DataGetDTO from a DataModel.
     * </p>
     * <p>
     *     If the dateVariables argument is set, the method will check if it contains the variable associated with the
     *     data. If this is the case, the value will be assumed to be a {@link ZonedDateTime} and will be converted to
     *     a {@link LocalDate} to be displayed correctly.
     * </p>
     * <p>
     *     The preferred way of performing this operation is by using {@link DataDAO#modelToDTO(DataModel)},
     *     or {@link DataDAO#modelListToDTO(ListWithPagination)} in the case of a list.
     * </p>
     *
     * @param model
     * @param dateVariables
     * @return
     */
    public static DataGetDTO getDtoFromModel(DataModel model, Set<URI> dateVariables) {
        DataGetDTO dto = new DataGetDTO();
        dto.fromModel(model, dateVariables);
        return dto;
    }

    private Object toLocalDate(Object modelValue) {
        if (modelValue instanceof ZonedDateTime) {
            return ((ZonedDateTime) modelValue).toLocalDate();
        }
        return modelValue;
    }
}

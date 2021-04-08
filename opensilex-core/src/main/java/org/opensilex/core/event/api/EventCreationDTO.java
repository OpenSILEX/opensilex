//******************************************************************************
//                          EventCreationDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************

package org.opensilex.core.event.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.StringUtils;
import org.opensilex.core.event.api.validation.EventDateTimeConstraint;
import org.opensilex.core.event.dal.EventModel;
import org.opensilex.sparql.model.time.InstantModel;
import org.opensilex.core.ontology.api.RDFObjectDTO;
import org.opensilex.server.rest.validation.ValidURI;
import org.opensilex.server.rest.validation.date.ValidOffsetDateTime;

import javax.validation.constraints.NotEmpty;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * @author Renaud COLIN
 */
@JsonPropertyOrder({
        "uri", "rdf_type","start", "end", "is_instant","description","targets","relations"
})
@EventDateTimeConstraint
public class EventCreationDTO extends RDFObjectDTO {

    @JsonProperty("rdf_type")
    protected URI type;

    @JsonProperty("start")
    protected String start;

    @JsonProperty("end")
    protected String end;

    @JsonProperty("targets")
    protected List<URI> concernedItems;

    @JsonProperty("description")
    protected String description;

    @JsonProperty("is_instant")
    protected Boolean isInstant;

    @ValidOffsetDateTime
    @ApiModelProperty(example = "2019-09-08T12:00:00+01:00")
    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    @ValidOffsetDateTime
    @ApiModelProperty(example = "2019-09-08T13:00:00+01:00")
    public String getEnd() {
        return end;
    }

    public void setEnd(String end) {
        this.end = end;
    }

    @NotEmpty
    @ValidURI
    @ApiModelProperty(required = true, value = "URI(s) of items concerned by this event")
    public List<URI> getConcernedItems() {
        return concernedItems;
    }

    public void setConcernedItems(List<URI> concernedItems) {
        this.concernedItems = concernedItems;
    }

    @ApiModelProperty(example = "2019-09-08T13:00:00+01:00")
    public String getDescription() {
        return description;
    }

    @ApiModelProperty(value = "Description of the event",example = "The pest attack lasted 20 minutes")
    public void setDescription(String description) {
        this.description = description;
    }

    @ApiModelProperty(required = true, value = "Indicate if the event is instant" ,example = "true")
    public Boolean getIsInstant() {
        return isInstant;
    }

    public void setIsInstant(boolean isInstant) {
        this.isInstant = isInstant;
    }

    @Override
    @ApiModelProperty(name = "rdf_type", value = "Event type URI", example = "http://www.opensilex.org/vocabulary/oeev#Irrigation")
    public URI getType() {
        return type;
    }

    @Override
    public void setType(URI type) {
        this.type = type;
    }

    protected <T extends EventModel> T toModel(T model){

        model.setUri(uri);
        model.setType(type);

        model.setConcernedItems(concernedItems);
        model.setDescription(description);
        model.setIsInstant(isInstant);


        if (!StringUtils.isEmpty(end)) {
            InstantModel endInstant = new InstantModel();
            endInstant.setDateTimeStamp(OffsetDateTime.parse(end));
            model.setEnd(endInstant);
        }

        if (!StringUtils.isEmpty(start)) {
            InstantModel instant = new InstantModel();
            instant.setDateTimeStamp(OffsetDateTime.parse(start));
            model.setStart(instant);
        }else{
            start = end;
        }

        return model;
    }

    public EventModel toModel() {
        return toModel(new EventModel());
    }

}
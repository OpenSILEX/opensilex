//******************************************************************************
//                          EventGetDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************

package org.opensilex.core.event.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.core.event.dal.EventModel;
import org.opensilex.server.rest.validation.Required;
import org.opensilex.server.rest.validation.ValidURI;
import org.opensilex.server.rest.validation.date.ValidOffsetDateTime;
import org.opensilex.sparql.response.ResourceDTO;

import javax.validation.constraints.NotEmpty;
import java.net.URI;
import java.util.List;

/**
 * @author Renaud COLIN
 */
@JsonPropertyOrder({
    "uri", "rdf_type", "rdf_type_name", "start", "end", "is_instant","description","targets","author"
})
public class EventGetDTO extends ResourceDTO<EventModel> {

    @JsonProperty("rdf_type")
    protected URI type;

    @JsonProperty("rdf_type_name")
    protected String typeLabel;

    @JsonProperty("start")
    protected String start;

    @JsonProperty("end")
    protected String end;

    @JsonProperty("targets")
    protected List<URI> concernedItems;

    @JsonProperty("description")
    protected String description;

    @JsonProperty("author")
    protected URI creator;

    @JsonProperty("is_instant")
    protected boolean isInstant;


    @Override
    @ValidURI
    @ApiModelProperty(value = "Event URI", example = "http://www.opensilex.org/event/12590c87-1c34-426b-a231-beb7acb33415")
    public URI getUri() {
        return uri;
    }

    @Override
    @ValidURI
    @ApiModelProperty(value = "Event type URI", example = "http://www.opensilex.org/vocabulary/oeev#Irrigation")
    public URI getType() {
        return type;
    }

    @Override
    @ApiModelProperty(value= "Event type name", example = "Move")
    public String getTypeLabel() {
        return typeLabel;
    }

    @ApiModelProperty(value = "Beginning of the event", example = "2019-09-08T12:00:00+01:00")
    @ValidOffsetDateTime
    @Required
    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    @ApiModelProperty(value = "End of the event", example = "2019-09-08T12:00:00+01:00")
    @ValidOffsetDateTime
    public String getEnd() {
        return end;
    }

    public void setEnd(String end) {
        this.end = end;
    }

    @ApiModelProperty(value = "URI(s) of items concerned by this event")
    @NotEmpty
    @ValidURI
    public List<URI> getConcernedItems() {
        return concernedItems;
    }

    public void setConcernedItems(List<URI> concernedItems) {
        this.concernedItems = concernedItems;
    }

    @ApiModelProperty(value = "Description of the event",example = "The pest attack lasted 20 minutes")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @ApiModelProperty(value = "Event creator URI" ,example = "http://opensilex.dev/users#Admin.OpenSilex")
    public URI getCreator() {
        return creator;
    }

    public void setCreator(URI creator) {
        this.creator = creator;
    }


    @ApiModelProperty(value = "Indicate if the event is instant" ,example = "false")
    public Boolean getIsInstant() {
        return isInstant;
    }

    public void setIsInstant(boolean isInstant) {
        this.isInstant = isInstant;
    }
    @Override
    public EventModel newModelInstance() {
        return new EventModel();
    }

    @Override
    public void fromModel(EventModel model) {
        uri = model.getUri();
        type = model.getType();
        typeLabel = model.getTypeLabel().getDefaultValue();

        if(model.getStart() != null){
            start = model.getStart().getDateTimeStamp().toString();
        }
        if(model.getEnd() != null){
            end = model.getEnd().getDateTimeStamp().toString();
        }

        description = model.getDescription();
        concernedItems = model.getConcernedItems();
        creator = model.getCreator();
        isInstant = model.getIsInstant();
    }
}

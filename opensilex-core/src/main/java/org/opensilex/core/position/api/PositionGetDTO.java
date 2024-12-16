package org.opensilex.core.position.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.core.location.api.LocationObservationDTO;
import org.opensilex.core.location.dal.LocationObservationModel;

import java.net.URI;
import java.util.Objects;

import org.opensilex.core.event.dal.move.MoveModel;

public class PositionGetDTO {

    @JsonProperty("event")
    private URI event;

    @JsonProperty("location")
    private LocationObservationDTO location;

    public PositionGetDTO(MoveModel model, LocationObservationModel locationModel) throws JsonProcessingException {

        event = model.getUri();

        if (Objects.nonNull(locationModel)) {
            location = LocationObservationDTO.getDTOFromModel(locationModel);
        }
    }

    public PositionGetDTO() {
    }

    @ApiModelProperty(value = "Move event which update the position", example = "http://www.opensilex.org/move/12590c87-1c34-426b-a231-beb7acb33415")
    public URI getEvent() {
        return event;
    }

    public void setEvent(URI event) {
        this.event = event;
    }

    public LocationObservationDTO getLocation() {
        return location;
    }

    public void setLocation(LocationObservationDTO location) {
        this.location = location;
    }
}

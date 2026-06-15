package org.opensilex.core.event.api.move;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.Info;
import org.opensilex.core.event.api.EventCreationDTO;
import org.opensilex.core.event.dal.move.MoveModel;
import org.opensilex.core.location.api.LocationObservationDTO;
import org.opensilex.core.location.dal.LocationObservationModel;
import org.opensilex.core.position.api.TargetPositionCreationDTO;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

@JsonPropertyOrder({
        "uri", "rdf_type", "start", "end", "is_instant", "description", "targets", "relations", "location", "from","to","targets_positions"
})
public class MoveCreationDTO extends EventCreationDTO {
    @JsonProperty("location")
    LocationObservationDTO location;

    @ApiModelProperty(value = "DEPRECATED: use 'location' instead", example="test:greenHouseA")
    @JsonProperty("from")
    private URI from;

    @ApiModelProperty(value = "DEPRECATED: use 'location' instead", example="test:greenHouseB")
    @JsonProperty("to")
    private URI to;

    @ApiModelProperty(value = "DEPRECATED: use 'location' instead")
    @JsonProperty("targets_positions")
    private List<TargetPositionCreationDTO> targetsPositions;

    public LocationObservationDTO getLocation() { return location; }

    public void setLocation(LocationObservationDTO location) {
        this.location = location;
    }

    public URI getFrom() {
        return from;
    }

    public void setFrom(URI from) {
        this.from = from;
    }

    public URI getTo() {
        return to;
    }

    public void setTo(URI to) {
        this.to = to;
    }

    public List<TargetPositionCreationDTO> getTargetsPositions() {
        return targetsPositions;
    }

    public void setTargetsPositions(List<TargetPositionCreationDTO> targetsPositions) {
        this.targetsPositions = targetsPositions;
    }

    public MoveModel toModel() {
        MoveModel model = super.toModel(new MoveModel());

        if (Objects.nonNull(location)) {
            //Set start and end date in function of the move's ones in case we didn't duplicate this information from the front
            if(location.getEndDate() == null){
                location.setEndDate(Instant.parse(super.getEnd()));
            }
            if(super.getStart() != null && location.getStartDate() == null){
                location.setStartDate(Instant.parse(super.getStart()));
            }
            LocationObservationModel locationObservationModel = location.newModel();
            model.setLocationObservation(locationObservationModel);
        }

        return model;
    }
}

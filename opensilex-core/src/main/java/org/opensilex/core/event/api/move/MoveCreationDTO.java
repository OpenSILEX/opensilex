package org.opensilex.core.event.api.move;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.opensilex.core.event.api.EventCreationDTO;
import org.opensilex.core.event.api.validation.MoveLocationOrPositionNotNullConstraint;
import org.opensilex.core.event.dal.move.MoveModel;
import org.opensilex.core.location.api.LocationObservationDTO;
import org.opensilex.core.location.dal.LocationObservationModel;

import java.time.Instant;
import java.util.Objects;

@JsonPropertyOrder({
        "uri", "rdf_type", "start", "end", "is_instant", "description", "targets", "relations", "location"
})
@MoveLocationOrPositionNotNullConstraint
public class MoveCreationDTO extends EventCreationDTO {

    @JsonProperty("location")
    LocationObservationDTO location;

    public LocationObservationDTO getLocation() {
        return location;
    }

    public void setLocation(LocationObservationDTO location) {
        this.location = location;
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

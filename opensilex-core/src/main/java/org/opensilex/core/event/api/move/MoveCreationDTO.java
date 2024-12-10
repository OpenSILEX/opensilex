package org.opensilex.core.event.api.move;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.opensilex.core.event.api.EventCreationDTO;
import org.opensilex.core.event.api.validation.MoveLocationOrPositionNotNullConstraint;
import org.opensilex.core.event.dal.move.MoveModel;
import org.opensilex.core.location.api.LocationObservationDTO;
import org.opensilex.core.location.dal.LocationObservationModel;

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
        MoveModel model = toModel(new MoveModel());

        if (Objects.nonNull(location)) {
            LocationObservationModel locationObservationModel = location.newModel();
            model.setLocationObservation(locationObservationModel);
        }

        return model;
    }
}

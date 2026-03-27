package org.opensilex.core.event.api.move;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.core.event.api.EventGetDTO;
import org.opensilex.core.event.dal.move.MoveModel;
import org.opensilex.core.location.api.LocationObservationDTO;

import java.util.Objects;

/**
 * @author Renaud COLIN
 */
@JsonPropertyOrder({
        "uri", "rdf_type", "rdf_type_name", "start", "end", "is_instant", "description", "targets", "author", "location"
})
public class MoveGetDTO extends EventGetDTO {

    @JsonProperty("location")
    private LocationObservationDTO location;

    public MoveGetDTO() {
    }

    public MoveGetDTO(MoveModel model) {

        super.fromModel(model);

        if (Objects.nonNull(model.getLocationObservation())) {
            location = LocationObservationDTO.getDTOFromModel(model.getLocationObservation());
        }
    }

    public LocationObservationDTO getLocation() {
        return location;
    }

    public void setLocation(LocationObservationDTO location) {
        this.location = location;
    }

    @ApiModelProperty(value = "Description of the move", example = "Move to greenhouse A")
    public String getDescription() {
        return description;
    }
}

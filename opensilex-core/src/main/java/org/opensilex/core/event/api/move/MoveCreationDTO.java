package org.opensilex.core.event.api.move;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.collections4.CollectionUtils;
import org.opensilex.core.event.api.EventCreationDTO;
import org.opensilex.core.event.api.validation.MoveLocationOrPositionNotNullConstraint;
import org.opensilex.core.event.dal.move.MoveModel;
import org.opensilex.core.location.api.LocationObservationDTO;
import org.opensilex.core.location.dal.LocationObservationModel;
import org.opensilex.core.organisation.dal.facility.FacilityModel;
import org.opensilex.core.position.api.TargetPositionCreationDTO;
import org.opensilex.core.event.dal.move.TargetPositionModel;
import org.opensilex.core.event.dal.move.MoveNosqlModel;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@JsonPropertyOrder({
    "uri", "rdf_type","start", "end", "is_instant","description","targets","relations","from","to","targets_positions"
})
@MoveLocationOrPositionNotNullConstraint
public class MoveCreationDTO extends EventCreationDTO {

    LocationObservationDTO location;

    //TODO: à retirer
    @JsonProperty("from")
    private URI from;

    @JsonProperty("to")
    private URI to;

    @JsonProperty("targets_positions")
    private List<TargetPositionCreationDTO> targetsPositions;

    @ApiModelProperty(example="test:greenHouseA")
    public URI getFrom() {
        return from;
    }

    public void setFrom(URI from) {
        this.from = from;
    }

    @ApiModelProperty(example="test:greenHouseB")
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

    public LocationObservationDTO getLocation() {
        return location;
    }

    public void setLocation(LocationObservationDTO location) {
        this.location = location;
    }

    public MoveModel toModel() {

        MoveModel model = toModel(new MoveModel());

        if(location != null) {
            LocationObservationModel locationObservationModel = location.newModel();
            model.setLocationObservation(locationObservationModel);
        }

        //TODO: à retirer
        if(from != null && ! from.toString().isEmpty()){
            FacilityModel fromModel = new FacilityModel();
            fromModel.setUri(from);
            model.setFrom(fromModel);
        }
        if(to != null && ! to.toString().isEmpty()){
            FacilityModel toModel = new FacilityModel();
            toModel.setUri(to);
            model.setTo(toModel);
        }

        MoveNosqlModel noSqlModel = toNoSqlModel();
        if(noSqlModel != null){
            model.setNoSqlModel(noSqlModel);
        }

        return model;
    }

    //TODO: à retirer
    public MoveNosqlModel toNoSqlModel() {

        if (CollectionUtils.isEmpty(targetsPositions)) {
            return null;
        }

        List<TargetPositionModel> itemPositions = targetsPositions.stream()
                .map(TargetPositionCreationDTO::toModel)
                .collect(Collectors.toList());

        MoveNosqlModel moveNoSql = new MoveNosqlModel();
        moveNoSql.setTargetPositions(itemPositions);
        return moveNoSql;
    }
}

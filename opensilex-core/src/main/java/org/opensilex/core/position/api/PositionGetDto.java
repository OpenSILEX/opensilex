package org.opensilex.core.position.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.core.organisation.api.facitity.InfrastructureFacilityNamedDto;
import org.opensilex.core.position.dal.PositionModel;
import org.opensilex.core.position.dal.PositionNoSqlModel;

import java.net.URI;

public class PositionGetDto {

    @JsonProperty("event")
    private URI event;

    @JsonProperty("move_time")
    private String moveTime;

    @JsonProperty("from")
    private InfrastructureFacilityNamedDto from;

    @JsonProperty("to")
    private InfrastructureFacilityNamedDto to;

    @JsonProperty("position")
    private PositionNoSqlGetDto position;

    public PositionGetDto(PositionModel model) throws JsonProcessingException {

        event = model.getEventUri();
        if(model.getFrom() != null){
            from = new InfrastructureFacilityNamedDto(model.getFrom());
        }
        if(model.getTo() != null){
            to = new InfrastructureFacilityNamedDto(model.getTo());
        }
        moveTime = model.getMoveTime().toString();

        PositionNoSqlModel positionNoSqlModel = model.getPositionNoSqlModel();
        if(positionNoSqlModel == null){
            return;
        }
        position = new PositionNoSqlGetDto(positionNoSqlModel);
    }

    public PositionGetDto(){

    }

    @ApiModelProperty(value = "Move event which update the position", example = "http://www.opensilex.org/move/12590c87-1c34-426b-a231-beb7acb33415")
    public URI getEvent() {
        return event;
    }

    public void setEvent(URI event) {
        this.event = event;
    }

    @ApiModelProperty(value = "Move time", example = "2019-09-08T12:00:00+01:00")
    public String getMoveTime() {
        return moveTime;
    }

    public void setMoveTime(String moveTime) {
        this.moveTime = moveTime;
    }

    public InfrastructureFacilityNamedDto getFrom() {
        return from;
    }

    public void setFrom(InfrastructureFacilityNamedDto from) {
        this.from = from;
    }

    public InfrastructureFacilityNamedDto getTo() {
        return to;
    }

    public void setTo(InfrastructureFacilityNamedDto to) {
        this.to = to;
    }

    public PositionNoSqlGetDto getPosition() {
        return position;
    }

    public void setPosition(PositionNoSqlGetDto position) {
        this.position = position;
    }
}

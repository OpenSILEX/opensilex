package org.opensilex.core.position.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.core.event.dal.move.TargetPositionModel;
import org.opensilex.core.event.dal.move.PositionModel;

import java.net.URI;
import java.net.URISyntaxException;

public class TargetPositionGetDTO {

    @JsonProperty("target")
    private URI target;

    @JsonProperty("position")
    private PositionGetDetailDTO position;

    public TargetPositionGetDTO(TargetPositionModel model) throws URISyntaxException, JsonProcessingException {

        this.target = model.getTarget();

        PositionModel position = model.getPosition();
        if(position != null){
            this.position = new PositionGetDetailDTO(position);
        }
    }

    public TargetPositionGetDTO(){

    }

    @ApiModelProperty(example="test:plantA")
    public URI getTarget() {
        return target;
    }

    public void setTarget(URI target) {
        this.target = target;
    }

    public PositionGetDetailDTO getPosition() {
        return position;
    }

    public void setPosition(PositionGetDetailDTO position) {
        this.position = position;
    }
}

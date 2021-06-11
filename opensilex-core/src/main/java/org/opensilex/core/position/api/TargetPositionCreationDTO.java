package org.opensilex.core.position.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.core.event.dal.move.TargetPositionModel;
import org.opensilex.server.rest.validation.Required;

import javax.validation.Valid;
import java.net.URI;

@ApiModel
@Valid
public class TargetPositionCreationDTO {

    @JsonProperty("target")
    private URI target;

    @JsonProperty("position")
    private PositionCreationDTO position;

    @Required
    @ApiModelProperty(required = true, example = "test:plantA")
    public URI getTarget() {
        return target;
    }

    public void setTarget(URI target) {
        this.target = target;
    }

    @Required
    public PositionCreationDTO getPosition() {
        return position;
    }

    public void setPosition(PositionCreationDTO position) {
        this.position = position;
    }

    public TargetPositionModel toModel(){

        TargetPositionModel model = new TargetPositionModel();
        model.setTarget(target);
        model.setPosition(position.toModel());
        return model;
    }

}

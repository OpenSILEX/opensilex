package org.opensilex.core.variable.api.entity;

import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotNull;
import java.net.URI;

public class EntityUpdateDTO extends EntityCreationDTO {

    @NotNull
    @Override
    @ApiModelProperty(required = true, example = "http://opensilex.dev/set/variables/entity/Plant")
    public URI getUri() {
        return super.getUri();
    }

}

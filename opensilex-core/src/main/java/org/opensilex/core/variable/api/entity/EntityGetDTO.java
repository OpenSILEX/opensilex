package org.opensilex.core.variable.api.entity;

import io.swagger.annotations.ApiModelProperty;
import org.opensilex.core.variable.api.BaseVariableGetDTO;
import org.opensilex.core.variable.dal.EntityModel;

import java.net.URI;

public class EntityGetDTO extends BaseVariableGetDTO<EntityModel> {

    public EntityGetDTO(EntityModel model) {
        super(model);
    }

    public EntityGetDTO() {
    }

    @Override
    @ApiModelProperty(example = "http://opensilex.dev/set/variables/entity/Plant")
    public URI getUri() {
        return uri;
    }

    @Override
    @ApiModelProperty(example = "Plant")
    public String getName() {
        return name;
    }
}

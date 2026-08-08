package org.opensilex.core.variable.api.entity;

import io.swagger.v3.oas.annotations.media.Schema;
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
    @Schema(example = "http://opensilex.dev/set/variables/entity/Plant")
    public URI getUri() {
        return uri;
    }

    @Override
    @Schema(example = "Plant")
    public String getName() {
        return name;
    }
}

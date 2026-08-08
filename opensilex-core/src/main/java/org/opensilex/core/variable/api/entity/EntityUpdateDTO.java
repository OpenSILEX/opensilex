package org.opensilex.core.variable.api.entity;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotNull;
import java.net.URI;

public class EntityUpdateDTO extends EntityCreationDTO {

    @NotNull
    @Override
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "http://opensilex.dev/set/variables/entity/Plant")
    public URI getUri() {
        return super.getUri();
    }

}

package org.opensilex.core.variable.api.entity;

import javax.validation.constraints.NotNull;
import java.net.URI;

public class EntityUpdateDTO extends EntityCreationDTO {

    @NotNull
    @Override
    public URI getUri() {
        return super.getUri();
    }

}

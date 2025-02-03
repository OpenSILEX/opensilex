package org.opensilex.core.variable.api.dimension;

import javax.validation.constraints.NotNull;
import java.net.URI;

public class DimensionUpdateDTO extends DimensionCreationDTO {

    @NotNull
    @Override
    public URI getUri() {
        return super.getUri();
    }
}

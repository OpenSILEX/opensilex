package org.opensilex.core.event.api.move;

import javax.validation.constraints.NotNull;
import java.net.URI;

public class MoveUpdateDTO extends MoveCreationDTO {

    @Override
    @NotNull
    public URI getUri() {
        return super.getUri();
    }
}

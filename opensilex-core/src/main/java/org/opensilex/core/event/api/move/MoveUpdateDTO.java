package org.opensilex.core.event.api.move;

import org.opensilex.core.event.api.EventCreationDTO;

import javax.validation.constraints.NotNull;
import java.net.URI;

public class MoveEventUpdateDTO extends MoveEventCreationDTO {

    @Override
    @NotNull
    public URI getUri() {
        return super.getUri();
    }
}

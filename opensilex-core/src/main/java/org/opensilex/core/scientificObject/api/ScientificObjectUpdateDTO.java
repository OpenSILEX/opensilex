package org.opensilex.core.scientificObject.api;

import io.swagger.v3.oas.annotations.media.Schema;
import org.opensilex.server.rest.validation.ValidURI;

import javax.validation.constraints.NotNull;
import java.net.URI;

public class ScientificObjectUpdateDTO extends ScientificObjectCreationDTO {

    @Override
    @NotNull
    @ValidURI
    @Schema(description = "Scientific object URI", example = ScientificObjectAPI.SCIENTIFIC_OBJECT_EXAMPLE_URI, requiredMode = Schema.RequiredMode.REQUIRED)
    public URI getUri() {
        return uri;
    }
}

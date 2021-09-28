package org.opensilex.core.scientificObject.api;

import io.swagger.annotations.ApiModelProperty;
import org.opensilex.server.rest.validation.ValidURI;

import javax.validation.constraints.NotNull;
import java.net.URI;

public class ScientificObjectUpdateDTO extends ScientificObjectCreationDTO {

    @Override
    @NotNull
    @ValidURI
    @ApiModelProperty(value = "Scientific object URI", example = ScientificObjectAPI.SCIENTIFIC_OBJECT_EXAMPLE_URI, required = true)
    public URI getUri() {
        return uri;
    }
}

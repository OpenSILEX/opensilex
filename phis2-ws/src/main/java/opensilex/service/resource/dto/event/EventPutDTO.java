//******************************************************************************
//                             EventPutDTO.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 5 Mar. 2019
// Contact: andreas.garcia@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.dto.event;

import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotNull;
import opensilex.service.documentation.DocumentationAnnotation;
import opensilex.service.resource.validation.interfaces.URL;
import opensilex.service.model.Event;

/**
 * Event PUT DTO.
 * @author Andréas Garcia <andreas.garcia@inra.fr>
 */
public class EventPutDTO extends EventActionDTO {
        
    protected String uri;

    /**
     * Generates an event object from a DTO.
     * @return the Event model
     */
    @Override
    public Event createObjectFromDTO() {
        Event event = super.createObjectFromDTO();
        event.setUri(uri);
        return event;
    }

    @URL
    @NotNull
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_EVENT_URI)
    public String getUri() {
        return uri;
    }
}

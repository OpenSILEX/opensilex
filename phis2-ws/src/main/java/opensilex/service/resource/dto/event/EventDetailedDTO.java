//******************************************************************************
//                              EventDetailedDTO.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 15 Feb. 2019
// Contact: andreas.garcia@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.dto.event;

import java.util.ArrayList;
import opensilex.service.resource.dto.annotation.AnnotationDTO;
import opensilex.service.model.Event;

/**
 * Event DTO.
 * @author Andréas Garcia <andreas.garcia@inra.fr>
 */
public class EventDetailedDTO extends EventSimpleDTO {
    
    protected ArrayList<AnnotationDTO> annotations = new ArrayList<>();
    
    /**
     * Constructor to create a DTO from an Event model.
     * @param event 
     */
    public EventDetailedDTO(Event event) {
        super(event);
        event.getAnnotations().forEach((annotation) -> {
            annotations.add(new AnnotationDTO(annotation));
        });
    }
}

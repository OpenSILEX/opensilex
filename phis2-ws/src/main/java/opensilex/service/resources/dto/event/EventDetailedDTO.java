//******************************************************************************
//                         EventDetailedDTO.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 15 Feb., 2019
// Contact: andreas.garcia@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resources.dto.event;

import java.util.ArrayList;
import opensilex.service.resources.dto.annotation.AnnotationDTO;
import opensilex.service.view.model.phis.Event;

/**
 * DTO representing an event detailed
 * @author Andréas Garcia<andreas.garcia@inra.fr>
 */
public class EventDetailedDTO extends EventSimpleDTO {
    
    protected ArrayList<AnnotationDTO> annotations = new ArrayList<>();
    
    /**
     * Constructor to create a DTO from an Event model
     * @param event 
     */
    public EventDetailedDTO(Event event) {
        super(event);
        event.getAnnotations().forEach((annotation) -> {
            annotations.add(new AnnotationDTO(annotation));
        });
    }
}

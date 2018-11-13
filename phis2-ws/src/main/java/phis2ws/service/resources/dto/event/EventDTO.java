//******************************************************************************
//                                 EventDTO.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 13 nov. 2018
// Contact: andreas.garcia@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.resources.dto.event;

import phis2ws.service.resources.dto.rdfResourceDefinition.RdfResourceDefinitionDTO;
import phis2ws.service.view.model.phis.Event;
import phis2ws.service.view.model.phis.RadiometricTarget;

/**
 * DTO for an event
 * 
 * @author Andréas Garcia<andreas.garcia@inra.fr>
 */
public class EventDTO extends RdfResourceDefinitionDTO {
    
    /**
     * Constructor to create DTO from an Event model
     * @param event 
     */
    public EventDTO(Event event) {
        super(event);
    }
    
    /**
     * Generates a RadiometricTarget model with the information of this
     * @return the model RadiometricTarget
     */
    public Event createEventFromDTO() {
        Event event = new Event(getLabel(), getUri(), "");
        
        getProperties().forEach((property) -> {
            event.addProperty(property.createObjectFromDTO());
        });
        
        return event;
    }
}

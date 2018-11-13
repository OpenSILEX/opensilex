//******************************************************************************
//                                 EventDTO.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 13 nov. 2018
// Contact: andreas.garcia@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.resources.dto.event;

import org.joda.time.DateTime;
import phis2ws.service.resources.dto.manager.AbstractVerifiedClass;
import phis2ws.service.view.model.phis.Event;

/**
 * DTO representing an event
 * 
 * @author Andréas Garcia<andreas.garcia@inra.fr>
 */
public class EventDTO extends AbstractVerifiedClass {
    
    protected String uri;
    protected String label;
    protected DateTime dateTime;
    
    /**
     * Constructor to create DTO from an Event model
     * @param event 
     */
    public EventDTO(Event event) {
        this.uri = event.getUri();
        this.label = event.getLabel();
        this.dateTime = event.getDateTime();
    }

    /**
     * Generates an event model from de DTO
     * @return the Event model
     */
    @Override
    public Event createObjectFromDTO() {
        return new Event(this.uri, this.label, this.dateTime);
    }
}

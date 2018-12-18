//******************************************************************************
//                                 EventDTO.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 13 nov. 2018
// Contact: andreas.garcia@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.resources.dto.event;

import java.util.ArrayList;
import java.util.HashMap;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import phis2ws.service.configuration.DateFormats;
import phis2ws.service.resources.dto.manager.AbstractVerifiedClass;
import phis2ws.service.utils.dates.Dates;
import phis2ws.service.view.model.phis.Event;
import phis2ws.service.view.model.phis.Property;

/**
 * DTO representing an event
 * 
 * @author Andréas Garcia<andreas.garcia@inra.fr>
 */
public class EventDTO extends AbstractVerifiedClass {
    
    private final String uri;
    private final String type;
    private final ArrayList<HashMap<String, ArrayList<String>>> concernsList;
    private final String dateTimeString;
    private final ArrayList<Property> properties;
    
    /**
     * Constructor to create DTO from an Event model
     * @param event 
     */
    public EventDTO(Event event) {
        this.uri = event.getUri();
        this.type = event.getType();
        this.concernsList = event.getConcernsList();
        
        DateTime eventDateTime = event.getDateTime();
        if(eventDateTime != null){
            this.dateTimeString = DateTimeFormat
                    .forPattern(DateFormats.DATETIME_JSON_SERIALISATION_FORMAT)
                    .print(eventDateTime);
        }
        else{
            this.dateTimeString = null;
        }
        this.properties = event.getProperties();
    }

    /**
     * Generates an event model from de DTO
     * @return the Event model
     */
    @Override
    public Event createObjectFromDTO() {
        return new Event(
                this.uri
                , this.type
                , this.concernsList
                , Dates.stringToDateTimeWithGivenPattern(
                    this.dateTimeString
                    , DateFormats.DATETIME_JSON_SERIALISATION_FORMAT)
                , this.properties);
    }
}

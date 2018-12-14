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
import org.joda.time.format.DateTimeFormatter;
import phis2ws.service.configuration.DateFormats;
import phis2ws.service.resources.dto.manager.AbstractVerifiedClass;
import phis2ws.service.utils.dates.Dates;
import phis2ws.service.view.model.phis.Event;

/**
 * DTO representing an event
 * 
 * @author Andréas Garcia<andreas.garcia@inra.fr>
 */
public class EventDTO extends AbstractVerifiedClass {
    
    private final String uri;
    private final String type;
    private final ArrayList<String> concernsList;
    private final String dateTimeString;
    private final HashMap<String, String> subclassSpecificProperties;
    
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
        this.subclassSpecificProperties = event.getSubclassSpecificProperties();
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
                , this.subclassSpecificProperties);
    }
}

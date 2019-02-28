//******************************************************************************
//                         EventSimpleDTO.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 13 nov. 2018
// Contact: andreas.garcia@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.resources.dto.event;

import java.util.ArrayList;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import phis2ws.service.configuration.DateFormat;
import phis2ws.service.resources.dto.ConcernedItemWithLabelsDTO;
import phis2ws.service.resources.dto.manager.AbstractVerifiedClass;
import phis2ws.service.resources.dto.rdfResourceDefinition.PropertyDTO;
import phis2ws.service.resources.validation.interfaces.URL;
import phis2ws.service.utils.dates.Dates;
import phis2ws.service.view.model.phis.ConcernedItem;
import phis2ws.service.view.model.phis.Event;
import phis2ws.service.view.model.phis.Property;

/**
 * DTO representing a event with the basic information
 * @author Andréas Garcia<andreas.garcia@inra.fr>
 */
public class EventSimpleDTO extends AbstractVerifiedClass {
    
    @URL
    private final String uri;
    
    @URL
    private final String type;
    private final ArrayList<ConcernedItemWithLabelsDTO> concernedItems = new ArrayList<>();
    private final String date;
    protected ArrayList<PropertyDTO> properties = new ArrayList<>();
    
    /**
     * Constructor to create a DTO from an Event model
     * @param event 
     */
    public EventSimpleDTO(Event event) {
        this.uri = event.getUri();
        this.type = event.getType();
        event.getConcernedItems().forEach((concernedItem) -> {
            this.concernedItems.add(new ConcernedItemWithLabelsDTO(concernedItem));
        });
        
        DateTime eventDateTime = event.getDateTime();
        if (eventDateTime != null){
            this.date = DateTimeFormat
                    .forPattern(DateFormat.YMDTHMSZZ.toString())
                    .print(eventDateTime);
        }
        else{
            this.date = null;
        }
        event.getProperties().forEach((property) -> {
            properties.add(new PropertyDTO(property));
        });
    }

    /**
     * Generates an event model from de DTO
     * @return the Event model
     */
    @Override
    public Event createObjectFromDTO() {
        
        ArrayList<Property> modelProperties = new ArrayList<>();
        this.properties.forEach((property) -> {
            modelProperties.add(property.createObjectFromDTO());
        });
        
        ArrayList<ConcernedItem> modelConcernedItems = new ArrayList<>();
        this.concernedItems.forEach((concernedItem) -> {
            modelConcernedItems.add(concernedItem.createObjectFromDTO());
        });
        
        DateTime dateTime = Dates.stringToDateTimeWithGivenPattern(this.date, DateFormat.YMDTHMSZZ.toString());
        
        return new Event(this.uri, this.type, modelConcernedItems, dateTime, modelProperties, null);
    }
}

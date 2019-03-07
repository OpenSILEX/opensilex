//******************************************************************************
//                         EventSimpleDTO.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 13 nov. 2018
// Contact: andreas.garcia@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.resources.dto.event;

import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import phis2ws.service.configuration.DateFormat;
import phis2ws.service.documentation.DocumentationAnnotation;
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
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_EVENT_URI)
    private String uri;
    
    @URL
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_EVENT_TYPE)
    private String type;
    
    protected ArrayList<ConcernedItemWithLabelsDTO> concernedItems = new ArrayList<>();
    
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_EVENT_DATE)
    private String date;
    
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

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public ArrayList<ConcernedItemWithLabelsDTO> getConcernedItems() {
        return concernedItems;
    }

    public void setConcernedItems(ArrayList<ConcernedItemWithLabelsDTO> concernedItems) {
        this.concernedItems = concernedItems;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public ArrayList<PropertyDTO> getProperties() {
        return properties;
    }

    public void setProperties(ArrayList<PropertyDTO> properties) {
        this.properties = properties;
    }
}

//******************************************************************************
//                              EventDTO.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 13 nov. 2018
// Contact: andreas.garcia@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.dto.event;

import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import opensilex.service.configuration.DateFormat;
import opensilex.service.documentation.DocumentationAnnotation;
import opensilex.service.resource.dto.ConcernedItemWithLabelsDTO;
import opensilex.service.resource.dto.manager.AbstractVerifiedClass;
import opensilex.service.resource.dto.rdfResourceDefinition.PropertyDTO;
import opensilex.service.resource.validation.interfaces.URL;
import opensilex.service.utils.date.Dates;
import opensilex.service.model.ConcernedItem;
import opensilex.service.model.Event;
import opensilex.service.model.Property;

/**
 * Event DTO.
 * @author Andréas Garcia<andreas.garcia@inra.fr>
 */
public class EventDTO extends AbstractVerifiedClass {
    
    @URL
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_EVENT_URI)
    private String uri;
    
    /**
     * //SILEX:info
     * "type" can not be used as a field name in DTOs due to XML interpretation issues.
     * @see https://stackoverflow.com/questions/33104232/eclipselink-missing-class-for-indicator-field-value-of-typ
     * //\
     */
    @URL
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_EVENT_TYPE)
    private String rdfType;
    
    protected ArrayList<ConcernedItemWithLabelsDTO> concernedItems = new ArrayList<>();
    
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_EVENT_DATE)
    private String date;
    
    protected ArrayList<PropertyDTO> properties = new ArrayList<>();
    
    /**
     * Constructor from an Event model.
     * @param event 
     */
    public EventDTO(Event event) {
        this.uri = event.getUri();
        this.rdfType = event.getType();
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
     * Generates an event model from a DTO.
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
        
        return new Event(this.uri, this.rdfType, modelConcernedItems, dateTime, modelProperties, null);
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getRdfType() {
        return rdfType;
    }

    public void setRdfType(String type) {
        this.rdfType = type;
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

//******************************************************************************
//                                 EventPutDTO.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 5 Mar. 2019
// Contact: andreas.garcia@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.dto.event;

import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import org.joda.time.DateTime;
import opensilex.service.configuration.DateFormat;
import opensilex.service.documentation.DocumentationAnnotation;
import opensilex.service.resource.dto.manager.AbstractVerifiedClass;
import opensilex.service.resource.dto.rdfResourceDefinition.PropertyDTO;
import opensilex.service.resource.validation.interfaces.Date;
import opensilex.service.resource.validation.interfaces.URL;
import opensilex.service.utils.date.Dates;
import opensilex.service.model.ConcernedItem;
import opensilex.service.model.Event;
import opensilex.service.model.Property;
import opensilex.service.resource.validation.interfaces.Required;

/**
 * Event PUT DTO.
 * @author Andréas Garcia <andreas.garcia@inra.fr>
 */
public class EventPutDTO extends AbstractVerifiedClass {
        
    protected String uri;
    protected String rdfType;
    protected ArrayList<String> concernedItemsUris;
    protected String date;
    protected ArrayList<PropertyDTO> properties;

    public EventPutDTO() {
        this.properties = new ArrayList<>();
    }

    /**
     * Generates an event object from a DTO.
     * @return the Event model
     */
    @Override
    public Event createObjectFromDTO() {
        
        ArrayList<Property> propertyObjects = new ArrayList<>();
        this.properties.forEach((property) -> {
            propertyObjects.add(property.createObjectFromDTO());
        });
        
        ArrayList<ConcernedItem> modelConcernedItems = new ArrayList<>();
        this.concernedItemsUris.forEach((concernedItemUri) -> {
            modelConcernedItems.add(new ConcernedItem(concernedItemUri, null, null, this.uri));
        });
        
        DateTime dateTime = Dates.stringToDateTimeWithGivenPattern(this.date, DateFormat.YMDTHMSZZ.toString());
        
        return new Event(this.uri, this.rdfType, modelConcernedItems, dateTime, propertyObjects, new ArrayList<>());
    }

    @URL
    @NotNull
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_EVENT_URI)
    public String getUri() {
        return uri;
    }

    @URL
    @NotNull
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_EVENT_TYPE)
    public String getRdfType() {
        return rdfType;
    }

    public void setRdfType(String rdfType) {
        this.rdfType = rdfType;
    }

    @URL
    @NotNull
    @NotEmpty
    public ArrayList<String> getConcernedItemsUris() {
        return concernedItemsUris;
    }

    public void setConcernedItemsUris(ArrayList<String> concernedItemsUris) {
        this.concernedItemsUris = concernedItemsUris;
    }

    @Date(DateFormat.YMDTHMSZZ)
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_EVENT_DATE)
    @Required
    @NotNull
    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    @Valid
    @NotNull
    public ArrayList<PropertyDTO> getProperties() {
        return properties;
    }

    public void setProperties(ArrayList<PropertyDTO> properties) {
        this.properties = properties;
    }
}

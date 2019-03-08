//******************************************************************************
//                         EventPostDTO.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 5 March, 2019
// Contact: andreas.garcia@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.resources.dto.event;

import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.joda.time.DateTime;
import phis2ws.service.configuration.DateFormat;
import phis2ws.service.documentation.DocumentationAnnotation;
import phis2ws.service.ontologies.Oa;
import phis2ws.service.resources.dto.manager.AbstractVerifiedClass;
import phis2ws.service.resources.dto.rdfResourceDefinition.PropertyDTO;
import phis2ws.service.resources.validation.interfaces.Date;
import phis2ws.service.resources.validation.interfaces.URL;
import phis2ws.service.utils.dates.Dates;
import phis2ws.service.view.model.phis.Annotation;
import phis2ws.service.view.model.phis.ConcernedItem;
import phis2ws.service.view.model.phis.Event;
import phis2ws.service.view.model.phis.Property;

/**
 * DTO representing a event with the basic information
 * @author Andréas Garcia<andreas.garcia@inra.fr>
 */
public class EventPostDTO extends AbstractVerifiedClass {
    
    private final String EVENT_POST_DEFAULT_ANNOTATION_MOTIVATION_INSTANCE = Oa.INSTANCE_DESCRIBING.toString();
    
    private String rdfType;
    private String description;
    private String creator;
    private ArrayList<String> concernedItemsUris;
    private String date;
    private ArrayList<PropertyDTO> properties;

    public EventPostDTO() {
        this.properties = new ArrayList<>();
    }

    /**
     * Generate an event model from de DTO
     * @return the Event model
     */
    @Override
    public Event createObjectFromDTO() {
        
        ArrayList<Property> modelProperties = new ArrayList<>();
        this.properties.forEach((property) -> {
            modelProperties.add(property.createObjectFromDTO());
        });
        
        ArrayList<ConcernedItem> modelConcernedItems = new ArrayList<>();
        this.concernedItemsUris.forEach((concernedItemUri) -> {
            modelConcernedItems.add(new ConcernedItem(concernedItemUri, null, null));
        });
        
        DateTime dateTime = Dates.stringToDateTimeWithGivenPattern(this.date, DateFormat.YMDTHMSZZ.toString());
        
        ArrayList<String> annotationBodyValues = new ArrayList();
        annotationBodyValues.add(description);
        String annotationMotivation = EVENT_POST_DEFAULT_ANNOTATION_MOTIVATION_INSTANCE;
        Annotation eventAnnotation = new Annotation(null, DateTime.now(), creator, annotationBodyValues, annotationMotivation, null);
        
        ArrayList<Annotation> eventAnnotations = new ArrayList<>();
        eventAnnotations.add(eventAnnotation);
        
        return new Event(null, this.rdfType, modelConcernedItems, dateTime, modelProperties, eventAnnotations);
    }

    @URL
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_EVENT_TYPE)
    public String getRdfType() {
        return rdfType;
    }

    public void setRdfType(String rdfType) {
        this.rdfType = rdfType;
    }

    @URL
    public ArrayList<String> getConcernedItemsUris() {
        return concernedItemsUris;
    }

    public void setConcernedItemsUris(ArrayList<String> concernedItemsUris) {
        this.concernedItemsUris = concernedItemsUris;
    }

    @Date(DateFormat.YMDTHMSZZ)
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_EVENT_DATE)
    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    @Valid
    public ArrayList<PropertyDTO> getProperties() {
        return properties;
    }

    public void setProperties(ArrayList<PropertyDTO> properties) {
        this.properties = properties;
    }

    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_EVENT_DESCRIPTION)
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @URL
    @NotNull
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_ANNOTATION_CREATOR)
    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }
}

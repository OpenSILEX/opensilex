//******************************************************************************
//                               EventPostDTO.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 5 Mar. 2019
// Contact: andreas.garcia@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.dto.event;

import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import javax.validation.constraints.NotNull;
import org.joda.time.DateTime;
import opensilex.service.documentation.DocumentationAnnotation;
import opensilex.service.ontology.Oa;
import opensilex.service.resource.validation.interfaces.URL;
import opensilex.service.model.Annotation;
import opensilex.service.model.Event;

/**
 * Event POST DTO.
 * @author Andréas Garcia <andreas.garcia@inra.fr>
 */
public class EventPostDTO extends EventActionDTO {
    
    private final String EVENT_POST_DEFAULT_ANNOTATION_MOTIVATION_INSTANCE = Oa.INSTANCE_DESCRIBING.toString();
    
    private String description;
    private String creator;

    /**
     * Generates an event model from de DTO.
     * @return the Event model
     */
    @Override
    public Event createObjectFromDTO() {
        
        Event event = super.createObjectFromDTO();
        
        ArrayList<String> annotationBodyValues = new ArrayList();
        annotationBodyValues.add(description);
        String annotationMotivation = EVENT_POST_DEFAULT_ANNOTATION_MOTIVATION_INSTANCE;
        Annotation eventAnnotation = new Annotation(
                null, 
                DateTime.now(), 
                creator, 
                annotationBodyValues, 
                annotationMotivation, 
                new ArrayList<>());
        
        ArrayList<Annotation> eventAnnotations = new ArrayList<>();
        eventAnnotations.add(eventAnnotation);
        event.setAnnotations(eventAnnotations);
        
        return event;
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

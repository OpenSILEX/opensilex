//******************************************************************************
//                                Event.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 12 Nov. 2018
// Contact: andreas.garcia@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.model;

import java.util.ArrayList;
import java.util.List;
import org.joda.time.DateTime;

/**
 * Event model.
 * @author Andréas Garcia <andreas.garcia@inra.fr>
 */
public class Event extends RdfResourceDefinition {
    
    /**
     * Type 
     * @example http://www.opensilex.org/vocabulary/oeev#MoveFrom
     */
    private String type;
    
    /**
     * Concerned items.
     */
    private List<ConcernedItem> concernedItems;
    
    /**
     * Instant.
     */
    private Instant instant;
    
    /**
     * Annotations.
     */
    private List<Annotation> annotations;

    /**
     * @param uri
     * @param type
     * @param concernedItems
     * @param instantUri
     * @param dateTime
     * @param properties
     * @param annotations
     */
    public Event(String uri, String type, List<ConcernedItem> concernedItems, String instantUri, DateTime dateTime, ArrayList<Property> properties, List<Annotation> annotations) {
        this.uri = uri;
        this.type = type;
        this.concernedItems = concernedItems;
        this.instant = new Instant(instantUri, dateTime);
        this.properties = properties;
        this.annotations = annotations;
    }

    /**
     * @param uri
     * @param type
     * @param concernedItemsUris
     * @param dateTime
     * @param properties
     * @param annotations
     */
    public Event(String uri, String type, List<String> concernedItemsUris, DateTime dateTime, ArrayList<Property> properties, List<Annotation> annotations) {
        this.uri = uri;
        this.type = type;
        
        this.concernedItems = new ArrayList<>();
        concernedItemsUris.forEach((concernedItemUri) -> {
            this.concernedItems.add(new ConcernedItem(concernedItemUri, null, null, uri));
        });
        
        this.instant = new Instant(null, dateTime);
        this.properties = properties;
        this.annotations = annotations;
    }

    /**
     * @param uri
     * @param type
     * @param concernedItems
     * @param dateTime
     * @param properties
     */
    public Event(String uri, String type, List<ConcernedItem> concernedItems, DateTime dateTime, ArrayList<Property> properties) {
        this.uri = uri;
        this.type = type;
        this.concernedItems = concernedItems;
        this.instant = new Instant(null, dateTime);
        this.properties = properties;
    }
    
    public void addConcernedItem(ConcernedItem concernedItem) {
        this.concernedItems.add(concernedItem);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<ConcernedItem> getConcernedItems() {
        return concernedItems;
    }

    public void setConcernedItems(List<ConcernedItem> concernedItems) {
        this.concernedItems = concernedItems;
    }

    public Instant getInstant() {
        return instant;
    }

    public void setInstant(Instant instant) {
        this.instant = instant;
    }

    public List<Annotation> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(List<Annotation> annotations) {
        this.annotations = annotations;
    }

    public void setUri(String uri) {
        super.setUri(uri);
        
        if(concernedItems != null) {
            concernedItems.forEach(concernedItem -> {
               concernedItem.setObjectLinked(uri);
            });
        }
        
        if(annotations != null) {
            annotations.forEach(annotation -> {
                boolean uriIsAlreadyTarget = false;
                if(annotation.getTargets() != null) {
                    for (String target : annotation.getTargets()) {
                        if(target.equals(uri)) {
                            uriIsAlreadyTarget = true;
                            break;
                        }
                    }
                    if (!uriIsAlreadyTarget) {
                        annotation.getTargets().add(uri);
                    }
                }
            });
        }
    }
}

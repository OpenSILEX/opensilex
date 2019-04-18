//******************************************************************************
//                                Event.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 12 Nov. 2018
// Contact: andreas.garcia@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.model;

import java.util.ArrayList;
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
    private ArrayList<ConcernedItem> concernedItems;
    
    /**
     * DateTime.
     */
    private DateTime dateTime;
    
    /**
     * Annotations.
     */
    private ArrayList<Annotation> annotations;

    /**
     * @param uri
     * @param type
     * @param concernedItems
     * @param dateTime
     * @param properties
     * @param annotations
     */
    public Event(String uri, String type, ArrayList<ConcernedItem> concernedItems, DateTime dateTime, ArrayList<Property> properties, ArrayList<Annotation> annotations) {
        this.uri = uri;
        this.type = type;
        this.concernedItems = concernedItems;
        this.dateTime = dateTime;
        this.properties = properties;
        this.annotations = annotations;
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

    public ArrayList<ConcernedItem> getConcernedItems() {
        return concernedItems;
    }

    public void setConcernedItems(ArrayList<ConcernedItem> concernedItems) {
        this.concernedItems = concernedItems;
    }

    public DateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(DateTime dateTime) {
        this.dateTime = dateTime;
    }

    public ArrayList<Annotation> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(ArrayList<Annotation> annotations) {
        this.annotations = annotations;
    }
}

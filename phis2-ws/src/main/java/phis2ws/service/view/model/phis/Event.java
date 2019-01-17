//******************************************************************************
//                                       Event.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 12 nov. 2018
// Contact: andreas.garcia@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.view.model.phis;

import java.util.ArrayList;
import org.joda.time.DateTime;

/**
 * Represents the Event model
 * @author Andréas Garcia <andreas.garcia@inra.fr>
 */
public class Event extends RdfResourceDefinition {
    
    // type eg. http://www.phenome-fppn.fr/vocabulary/2018/oeev#MoveFrom
    private String type;
    
    // concerned items
    private ArrayList<ConcernedItem> concernedItems;
    
    // dateTime 
    private DateTime dateTime;

    public Event(String uri, String type, ArrayList<ConcernedItem> concernedItems, DateTime dateTime, ArrayList<Property> properties) {
        this.uri = uri;
        this.type = type;
        this.concernedItems = concernedItems;
        this.dateTime = dateTime;
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
}

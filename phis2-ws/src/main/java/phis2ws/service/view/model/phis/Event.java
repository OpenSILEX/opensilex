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
    
    private String type;
    private ArrayList<ConcernItem> concernsItems;
    private DateTime dateTime;

    public Event(String uri, String type
            , ArrayList<ConcernItem> concernsItems
            , DateTime dateTime
            , ArrayList<Property> properties) {
        this.uri = uri;
        this.type = type;
        this.concernsItems = concernsItems;
        this.dateTime = dateTime;
        this.properties = properties;
    }
    
    public void addConcernsItem(ConcernItem concerns){
        this.concernsItems.add(concerns);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public ArrayList<ConcernItem> getConcernsItems() {
        return concernsItems;
    }

    public void setConcernsItems(ArrayList<ConcernItem> concernsItems) {
        this.concernsItems = concernsItems;
    }

    public DateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(DateTime dateTime) {
        this.dateTime = dateTime;
    }
}

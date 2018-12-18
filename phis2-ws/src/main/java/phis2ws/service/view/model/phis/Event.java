//******************************************************************************
//                                       Event.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 12 nov. 2018
// Contact: andreas.garcia@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.view.model.phis;

import java.util.ArrayList;
import java.util.HashMap;
import org.joda.time.DateTime;

/**
 * @author Andréas Garcia <andreas.garcia@inra.fr>
 */
public class Event extends RdfResourceDefinition {
    
    private String type;
    private ArrayList<HashMap<String, ArrayList<String>>> concernsList;
    private DateTime dateTime;

    public Event(String uri, String type, 
            ArrayList<HashMap<String, ArrayList<String>>> concernsList
            , DateTime dateTime
            , ArrayList<Property> subclassSpecificProperties) {
        this.uri = uri;
        this.type = type;
        this.concernsList = concernsList;
        this.dateTime = dateTime;
        this.properties = subclassSpecificProperties;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public ArrayList<HashMap<String, ArrayList<String>>> getConcernsList() {
        return concernsList;
    }

    public void setConcernsList(
            ArrayList<HashMap<String, ArrayList<String>>> concernsList) {
        this.concernsList = concernsList;
    }

    public DateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(DateTime dateTime) {
        this.dateTime = dateTime;
    }
}

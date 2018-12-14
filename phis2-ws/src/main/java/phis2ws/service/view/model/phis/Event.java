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
public class Event {
    
    private String uri;
    private String type;
    private ArrayList<String> concernsUris;
    private DateTime dateTime;
    private HashMap<String, String> subclassSpecificProperties;

    public Event(String uri, String type, ArrayList<String> concernsUris
            , DateTime dateTime, HashMap subclassSpecificProperties) {
        this.uri = uri;
        this.type = type;
        this.concernsUris = concernsUris;
        this.dateTime = dateTime;
        this.subclassSpecificProperties = subclassSpecificProperties;
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

    public ArrayList<String> getConcernsUris() {
        return concernsUris;
    }

    public void setConcernsUris(ArrayList<String> concernsUris) {
        this.concernsUris = concernsUris;
    }

    public DateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(DateTime dateTime) {
        this.dateTime = dateTime;
    }

    public HashMap getSubclassSpecificProperties() {
        return subclassSpecificProperties;
    }

    public void setSubclassSpecificProperties(
            HashMap subclassSpecificProperties) {
        this.subclassSpecificProperties = subclassSpecificProperties;
    }
}

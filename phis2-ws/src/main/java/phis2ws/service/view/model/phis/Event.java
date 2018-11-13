//******************************************************************************
//                                       Event.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 12 nov. 2018
// Contact: andreas.garcia@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.view.model.phis;

import org.joda.time.DateTime;

/**
 * This is the model for an event
 * @author Andréas Garcia <andreas.garcia@inra.fr>
 */
public class Event {
    /*
    TODO:has to be set abstract when subclasses like Displacement are 
    implemented
    */
    
    protected String uri;
    protected String label;
    protected DateTime dateTime;

    public Event(String uri, String label, DateTime dateTime) {
        this.uri = uri;
        this.label = label;
        this.dateTime = dateTime;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public DateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(DateTime dateTime) {
        this.dateTime = dateTime;
    }
}

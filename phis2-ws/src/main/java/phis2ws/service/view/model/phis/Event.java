//******************************************************************************
//                                       Event.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 12 nov. 2018
// Contact: andreas.garcia@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.view.model.phis;

/**
 * This is the model for the events
 * @author Andréas Garcia <andreas.garcia@inra.fr>
 */
public abstract class Event {

    protected String uri;
    
    protected String date;

    public Event(String uri, String date) {
        this.uri = uri;
        this.date = date;
    }

    public String getEventUri() {
        return uri;
    }

    public void setEventUri(String uri) {
        this.uri = uri;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}

//******************************************************************************
//                                       Event.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 12 nov. 2018
// Contact: andreas.garcia@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.view.model.phis;

import org.joda.time.DateTime;
import phis2ws.service.resources.validation.interfaces.Date;

/**
 * This is the model for the events
 * @author Andréas Garcia <andreas.garcia@inra.fr>
 */
public class Event extends RdfResourceDefinition {
    /*
    TODO:has to be set abstract when subclasses like Displacement are 
    implemented
    */
    
    protected DateTime dateTime;

    public Event(String uri, String label, DateTime dateTime) {
        this.uri = uri;
        this.label = label;
        this.dateTime = dateTime;
    }

    public DateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(DateTime dateTime) {
        this.dateTime = dateTime;
    }
    
    
}

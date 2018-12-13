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
 * @author Andréas Garcia <andreas.garcia@inra.fr>
 */
public class Event {
    
    private String uri;
    private String type;
    private String concerns;
    private DateTime dateTime;

    public Event(String uri, String type, String concerns
            , DateTime dateTime) {
        this.uri = uri;
        this.type = type;
        this.concerns = concerns;
        this.dateTime = dateTime;
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

    public String getConcerns() {
        return concerns;
    }

    public void setConcerns(String concerns) {
        this.concerns = concerns;
    }

    public DateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(DateTime dateTime) {
        this.dateTime = dateTime;
    }
}

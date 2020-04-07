//******************************************************************************
//                                Instant.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 12 Apr. 2019
// Contact: andreas.garcia@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.model;

import opensilex.service.ontology.Time;
import org.joda.time.DateTime;

/**
 * Event model.
 * @author Andréas Garcia <andreas.garcia@inra.fr>
 */
public class Instant extends RdfResourceDefinition {
    
    /**
     * Type 
     * @example http://www.opensilex.org/vocabulary/oeev#MoveFrom
     */
    private String type = Time.Instant.getURI();
    
    /**
     * DateTime.
     */
    private DateTime dateTime;

    /**
     * @param uri
     * @param dateTime
     */
    public Instant(String uri, DateTime dateTime) {
        this.uri = uri;
        this.dateTime = dateTime;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public DateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(DateTime dateTime) {
        this.dateTime = dateTime;
    }
}

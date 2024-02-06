//******************************************************************************
//                          ActivityModel.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: alice.boizet@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.provenance.dal;

import java.net.URI;
import java.time.Instant;
import org.bson.Document;

/**
 * Activity model
 * @author Alice Boizet
 */
public class ActivityModel {    
    URI rdfType;   

    URI uri;
    
    Instant startDate;
    
    Instant endDate;
    
    String offset;
    
    Document settings; 

    public URI getRdfType() {
        return rdfType;
    }

    public void setRdfType(URI rdfType) {
        this.rdfType = rdfType;
    }

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public Instant getStartDate() {
        return startDate;
    }

    public void setStartDate(Instant startDate) {
        this.startDate = startDate;
    }

    public Instant getEndDate() {
        return endDate;
    }

    public void setEndDate(Instant endDate) {
        this.endDate = endDate;
    }

    public String getOffset() {
        return offset;
    }

    public void setOffset(String offset) {
        this.offset = offset;
    }    

    public Document getSettings() {
        return settings;
    }

    public void setSettings(Document settings) {
        this.settings = settings;
    }
    
}

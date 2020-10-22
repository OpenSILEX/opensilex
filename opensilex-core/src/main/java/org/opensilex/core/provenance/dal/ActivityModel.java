//******************************************************************************
//                          ActivityModel.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: alice.boizet@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.provenance.dal;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import org.bson.Document;

/**
 * Activity model
 * @author Alice Boizet
 */
public class ActivityModel {    
    @JsonProperty("rdfType")
    URI type;   

    String startedAtTime;
    
    String endedAtTime;
    
    Document settings; 

    public URI getType() {
        return type;
    }

    public void setType(URI type) {
        this.type = type;
    }
    
    public String getStartedAtTime() {
        return startedAtTime;
    }

    public void setStartedAtTime(String startedAtTime) {
        this.startedAtTime = startedAtTime;
    }

    public String getEndedAtTime() {
        return endedAtTime;
    }

    public void setEndedAtTime(String endedAtTime) {
        this.endedAtTime = endedAtTime;
    }

    public Document getSettings() {
        return settings;
    }

    public void setSettings(Document settings) {
        this.settings = settings;
    }
    
}

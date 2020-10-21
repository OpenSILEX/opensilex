//******************************************************************************
//                          ActivityModel.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: alice.boizet@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.provenance.dal;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.Map;
import javax.jdo.annotations.Column;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;

/**
 * Activity model
 * @author Alice Boizet
 */
@PersistenceCapable(embeddedOnly="true")
public class ActivityModel {    
    @JsonProperty("rdfType")
    @Column(name="rdf:type")
    URI type;   

    String startedAtTime;
    
    String endedAtTime;
    
    @Persistent(defaultFetchGroup="true")
    Map settings; 

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

    public Map getSettings() {
        return settings;
    }

    public void setSettings(Map settings) {
        this.settings = settings;
    }
    
}

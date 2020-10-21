//******************************************************************************
//                          AgentModel.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: alice.boizet@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.provenance.dal;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.Map;
import javax.jdo.annotations.Column;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PersistenceCapable;

/**
 * Agent model
 * @author Alice Boizet
 */
@PersistenceCapable(embeddedOnly="true")
public class AgentModel {    
    @JsonProperty("rdfType")
    @Column(name="rdf:type")
    URI type;

    @Persistent(defaultFetchGroup="true")
    Map settings; 
    
    URI uri;

    public URI getType() {
        return type;
    }

    public void setType(URI type) {
        this.type = type;
    }

    public Map getSettings() {
        return settings;
    }

    public void setSettings(Map settings) {
        this.settings = settings;
    }

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

}

//******************************************************************************
//                          AgentModel.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: alice.boizet@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.provenance.dal;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import org.bson.Document;

/**
 * Agent model
 * @author Alice Boizet
 */
public class AgentModel {    
    @JsonProperty("rdfType")
    URI type;

    Document settings; 
    
    URI uri;

    public URI getType() {
        return type;
    }

    public void setType(URI type) {
        this.type = type;
    }

    public Document getSettings() {
        return settings;
    }

    public void setSettings(Document settings) {
        this.settings = settings;
    }

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

}

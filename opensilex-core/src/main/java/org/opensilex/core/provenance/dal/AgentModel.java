//******************************************************************************
//                          AgentModel.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: alice.boizet@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.provenance.dal;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModelProperty;
import java.net.URI;
import org.bson.Document;

/**
 * Agent model
 * @author Alice Boizet
 */
@JsonPropertyOrder({"uri", "rdf_type", "settings"})
public class AgentModel {    
    @JsonProperty("rdf_type")
    @ApiModelProperty(value = "activity type defined in the ontology", example = "oeso:Sensor")
    URI rdfType;

    @ApiModelProperty(value = "a key value system to store agent parameters")
    Document settings; 
    
    @ApiModelProperty(value = "agent uri", example = "http://sensor/s01")
    URI uri;

    public URI getRdfType() {
        return rdfType;
    }

    public void setRdfType(URI rdfType) {
        this.rdfType = rdfType;
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

/*
 *  *************************************************************************************
 *  AbstractProvEntity.java
 *  OpenSILEX - Licence AGPL V3.0 -  https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright @ INRAE 2024
 * Contact :  user@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 * ************************************************************************************
 */

package org.opensilex.core.provenance.dal;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.bson.Document;

import java.net.URI;

public abstract class GlobalProvenanceEntity {

    @JsonProperty("rdf_type")
    URI rdfType;

    URI uri;

    @ApiModelProperty(value = "a key value system to store parameters")
    Document settings;

    public URI getRdfType() {
        return rdfType;
    }

    public GlobalProvenanceEntity setRdfType(URI rdfType) {
        this.rdfType = rdfType;
        return this;
    }

    public URI getUri() {
        return uri;
    }

    public GlobalProvenanceEntity setUri(URI uri) {
        this.uri = uri;
        return this;
    }

    public Document getSettings() {
        return settings;
    }

    public GlobalProvenanceEntity setSettings(Document settings) {
        this.settings = settings;
        return this;
    }
}

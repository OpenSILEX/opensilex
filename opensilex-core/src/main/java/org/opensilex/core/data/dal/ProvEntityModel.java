//******************************************************************************
//                          ProvEntityModel.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: alice.boizet@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.data.dal;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;

/**
 * Entity model used in "prov:used"
 * @author Alice Boizet
 */
public class ProvEntityModel {
    @JsonProperty("rdfType")
    URI type;
    
    URI uri;

    public URI getType() {
        return type;
    }

    public void setType(URI type) {
        this.type = type;
    }

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }
        
}

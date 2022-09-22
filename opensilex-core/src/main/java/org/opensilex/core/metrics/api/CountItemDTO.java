//******************************************************************************
//                          CountItemDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2022
// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.metrics.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.net.URI;

/**
 *
 * @author Arnaud Charleroy
 */
@JsonPropertyOrder({"uri", "name","type","count"})
public class CountItemDTO {
    
    @JsonProperty("uri")
    private URI uri;
    
    @JsonProperty("type")
    private URI type;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("count")
    private Integer count;
    
    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public URI getType() {
        return type;
    }

    public void setType(URI type) {
        this.type = type;
    } 
    
    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }
    
    
}

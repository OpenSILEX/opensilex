//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************

package org.opensilex.elastic.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import java.net.URI;
import org.opensilex.server.rest.validation.Required;


/**
 *
 * @author assarar
 */
public class GlobalSearchDTO {
        
    @JsonProperty("uri")
    protected URI uri;
    
    @JsonProperty("name")
    protected String name;

    @JsonProperty("description")
    protected String description;
    
    //@JsonProperty("rdf_type")
    //protected URI rdfType;
    
   //@JsonProperty("rdf_type")
    protected URI type;


 
    public URI getUri() {
        return uri;
    }

    public GlobalSearchDTO setUri(URI uri) {
        this.uri = uri;
        return this;
    }

    @Required
    @ApiModelProperty(example = "DROught-tolerant yielding PlantS", required=true)
    public String getName() {
        return name;
    }

    public GlobalSearchDTO setName(String name) {
        this.name = name;
        return this;
    }
    
    @ApiModelProperty(example = "DROPS aims at developing novel methods....")
    public String getDescription() {
        return description;
    }

    public GlobalSearchDTO setDescription(String description) {
        this.description = description;
        return this;
    }
    // public URI getRdfType() {
      //  return type;
    //}

    public void setType(URI rdfType) {
        this.type = rdfType;
    }
     public URI getType() {
        return type;
    }
  
}

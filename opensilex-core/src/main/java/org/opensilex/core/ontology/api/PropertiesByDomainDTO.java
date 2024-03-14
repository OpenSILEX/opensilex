package org.opensilex.core.ontology.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.opensilex.sparql.response.ResourceTreeDTO;

import java.net.URI;
import java.util.List;

public class PropertiesByDomainDTO {

    PropertiesByDomainDTO(URI domain, List<ResourceTreeDTO> properties){
        this.domain = domain;
        this.properties = properties;
    }

    @JsonProperty("domain")
    URI domain;

    @JsonProperty("properties")
    List<ResourceTreeDTO> properties;

    public URI getDomain() {
        return domain;
    }

    public void setDomain(URI domain) {
        this.domain = domain;
    }

    public List<ResourceTreeDTO> getProperties() {
        return properties;
    }

    public void setProperties(List<ResourceTreeDTO> properties) {
        this.properties = properties;
    }
}

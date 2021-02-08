package org.opensilex.sparql.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;

import java.net.URI;


@JsonPropertyOrder({
    "uri", "name"
})
public class ObjectNamedResourceDTO {

    @JsonProperty("uri")
    protected URI uri;

    @JsonProperty("name")
    protected String name;

    public ObjectNamedResourceDTO() {
    }

    public ObjectNamedResourceDTO(SPARQLNamedResourceModel<?> model) {
        this.uri = model.getUri();
        this.name = model.getName();
    }

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


}

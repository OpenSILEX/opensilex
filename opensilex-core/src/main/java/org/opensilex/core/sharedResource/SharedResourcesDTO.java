package org.opensilex.core.sharedResource;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URI;

public class SharedResourcesDTO {

    @JsonProperty("uri")
    protected URI uri;

    @JsonProperty("label")
    protected String label;


    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {this.label = label;}

}

package org.opensilex.core.variable.api.sharedResource;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.opensilex.server.rest.validation.ValidURI;

import java.net.URI;
import java.util.List;

public class ImportUrisDTO {

    @ValidURI
    protected List<URI> uris;

    @JsonProperty("resource")
    protected URI resource;

    public List<URI> getUris() {
        return uris;
    }

    public void setUris(List<URI> uris) {
        this.uris = uris;
    }

    public URI getResource() {
        return resource;
    }

    public void setResource(URI resource) {
        this.resource = resource;
    }
}

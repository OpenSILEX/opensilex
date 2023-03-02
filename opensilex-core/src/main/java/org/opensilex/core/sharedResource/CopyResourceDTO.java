package org.opensilex.core.sharedResource;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URI;
import java.util.List;

/**
 * Represent a request to copy a resource from a shared resource instance to the local instance. Resources are represented
 * by a list of their URIs
 *
 * @author Valentin Rigolle
 */
public class CopyResourceDTO {

    @JsonProperty("uris")
    protected List<URI> uris;

    @JsonProperty("sharedResourceInstance")
    protected URI sharedResourceInstance;

    public List<URI> getUris() {
        return uris;
    }

    public void setUris(List<URI> uris) {
        this.uris = uris;
    }

    public URI getSharedResourceInstance() {
        return sharedResourceInstance;
    }

    public void setSharedResourceInstance(URI sharedResourceInstance) {
        this.sharedResourceInstance = sharedResourceInstance;
    }
}

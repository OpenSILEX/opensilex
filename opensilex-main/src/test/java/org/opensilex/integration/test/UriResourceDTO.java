package org.opensilex.integration.test;

import io.swagger.v3.oas.annotations.media.Schema;
import org.opensilex.server.rest.validation.ValidURI;

import java.net.URI;

/**
 * Basic DTO with a URI for deserialization purposes
 */
public class UriResourceDTO {

    /**
     * uri
     */
    @ValidURI
    @Schema()
    protected URI uri;

    public UriResourceDTO() {

    }

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }
}

package org.opensilex.core.sharedResource;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.opensilex.core.config.SharedResourceInstanceItem;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

/**
 * Describes a Shared Resource Instance (SRI). An SRI has a URI and a label.
 *
 * @author Valentin Rigolle
 */
public class SharedResourceInstanceDTO {

    @JsonProperty("uri")
    protected URI uri;

    @JsonProperty("label")
    protected String label;

    public static SharedResourceInstanceDTO fromConfig(SharedResourceInstanceItem sharedResourceInstanceItem, String lang) {
        String label = Optional.ofNullable(sharedResourceInstanceItem.label().get(lang))
                .orElse(sharedResourceInstanceItem.uri());
        try {
            return new SharedResourceInstanceDTO()
                    .setUri(new URI(sharedResourceInstanceItem.uri()))
                    .setLabel(label);
        } catch (URISyntaxException e) {
            throw new RuntimeException("The URL " + sharedResourceInstanceItem.uri() + " is not valid for this shared resource : " + sharedResourceInstanceItem.label(), e);
        }
    }

    public URI getUri() {
        return uri;
    }

    public SharedResourceInstanceDTO setUri(URI uri) {
        this.uri = uri;
        return this;
    }

    public String getLabel() {
        return label;
    }

    public SharedResourceInstanceDTO setLabel(String label) {
        this.label = label;
        return this;
    }
}

package org.opensilex.security.user.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModel;

import java.net.URI;

@ApiModel
@JsonPropertyOrder({"type", "uri"})
public class FavoriteCreationDTO {

    @JsonProperty("type")
    protected String type;

    @JsonProperty("uri")
    protected URI uri;

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

}

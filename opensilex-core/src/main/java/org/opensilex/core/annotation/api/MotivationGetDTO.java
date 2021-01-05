package org.opensilex.core.annotation.api;

import io.swagger.annotations.ApiModelProperty;
import org.opensilex.core.annotation.dal.MotivationModel;

import java.net.URI;

public class MotivationGetDTO {

    protected URI uri;
    protected String name;

    public MotivationGetDTO() {
    }

    public MotivationGetDTO(MotivationModel model){
        uri = model.getUri();
        name = model.getName();
    }

    @ApiModelProperty(value = "URI of the motivation", example = "http://www.w3.org/ns/oa#describing")
    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    @ApiModelProperty(value = "Name of the annotation motivation", example = "describing")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

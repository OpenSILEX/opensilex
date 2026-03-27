package org.opensilex.core.experiment.api;

import io.swagger.annotations.ApiModelProperty;
import org.opensilex.core.experiment.dal.FundingModel;

import java.net.URI;

public class FundingGetDTO {

    protected URI uri;
    protected String name;

    public FundingGetDTO() {
    }

    public FundingGetDTO(FundingModel model){
        uri = model.getUri();
        name = model.getName();
    }

    @ApiModelProperty(value = "URI of the funding", example = "http://www.opensilex.org/vocabulary/oeso#anr")
    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    @ApiModelProperty(value = "Name of the funding", example = "anr")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

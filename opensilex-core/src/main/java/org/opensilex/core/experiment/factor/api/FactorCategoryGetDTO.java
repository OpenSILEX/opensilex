package org.opensilex.core.experiment.factor.api;

import io.swagger.annotations.ApiModelProperty;

import java.net.URI;
import org.opensilex.core.experiment.factor.dal.FactorCategorySKOSModel;

public class FactorCategoryGetDTO {

    protected URI uri;
    protected String name;

    public FactorCategoryGetDTO() {
    }

    public FactorCategoryGetDTO(FactorCategorySKOSModel model){
        uri = model.getUri();
        name = model.getName();
    }

    @ApiModelProperty(value = "URI of the factor category", example = "http://www.w3.org/ns/oa#describing")
    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    @ApiModelProperty(value = "Name of the factor category", example = "describing")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

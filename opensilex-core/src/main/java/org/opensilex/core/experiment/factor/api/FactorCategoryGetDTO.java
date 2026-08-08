package org.opensilex.core.experiment.factor.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.net.URI;
import org.opensilex.core.experiment.factor.dal.FactorCategoryModel;

public class FactorCategoryGetDTO {

    protected URI uri;
    protected String name;

    public FactorCategoryGetDTO() {
    }

    public FactorCategoryGetDTO(FactorCategoryModel model){
        uri = model.getUri();
        name = model.getName();
    }

    @Schema(description = "URI of the factor category", example = "http://www.w3.org/ns/oa#describing")
    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    @Schema(description = "Name of the factor category", example = "describing")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

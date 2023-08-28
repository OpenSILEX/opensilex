package org.opensilex.core.data.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.opensilex.server.rest.validation.ValidURI;

import java.net.URI;

public class SingleCriteriaDTO {

    private static final String variableField = "variable_uri";
    private static final String criteriaOperatorField = "criteria_operator";
    private static final String valueField = "value";

    @JsonProperty(variableField)
    @ValidURI
    @ApiModelProperty(value = "uri of the variable that criteria applies too", required = true)
    private URI variableUri;

    @JsonProperty(criteriaOperatorField)
    @ValidURI
    @ApiModelProperty(value = "uri of the criteria (<,>,<=,>= or =). Some subclass of oeso#MathmaticalOperator",
            required = true,
            example = "http://www.opensilex.org/vocabulary/oeso#LessThan")
    private URI criteria;

    @JsonProperty(valueField)
    @ApiModelProperty(value = "value to compare with", required = true)
    private String value;

    public URI getVariableUri() {
        return variableUri;
    }

    public void setVariableUri(URI variableUri) {
        this.variableUri = variableUri;
    }

    public URI getCriteria() {
        return criteria;
    }

    public void setCriteria(URI criteria) {
        this.criteria = criteria;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

}

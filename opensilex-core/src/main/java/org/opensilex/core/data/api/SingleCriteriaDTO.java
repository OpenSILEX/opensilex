package org.opensilex.core.data.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.opensilex.core.data.utils.MathematicalOperator;
import org.opensilex.server.rest.validation.ValidURI;

import java.net.URI;

public class SingleCriteriaDTO {

    private static final String variableField = "variable_uri";
    private static final String criteriaOperatorField = "criteria_operator";
    private static final String valueField = "value";

    @JsonProperty(variableField)
    @ValidURI
    @Schema(description = "uri of the variable that criteria applies too", requiredMode = Schema.RequiredMode.REQUIRED)
    private URI variableUri;

    @JsonProperty(criteriaOperatorField)
    @Schema(description = "The criteria (LessThan,LessOrEqualThan,MoreThan, MoreOrEqualThan,EqualToo,NotMeasured).",
            required = true,
            example = "<=")
    private MathematicalOperator criteria;

    @JsonProperty(valueField)
    @Schema(description = "value to compare with", requiredMode = Schema.RequiredMode.REQUIRED)
    private String value;

    public URI getVariableUri() {
        return variableUri;
    }

    public void setVariableUri(URI variableUri) {
        this.variableUri = variableUri;
    }

    public MathematicalOperator getCriteria() {
        return criteria;
    }

    public void setCriteria(MathematicalOperator criteria) {
        this.criteria = criteria;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

}

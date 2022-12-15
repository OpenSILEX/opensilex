package org.opensilex.core.csv.api;

import io.swagger.annotations.ApiModelProperty;

/**
 * @author rcolin
 */
public class CsvImportDTO {

    @ApiModelProperty(value = "Validation token provided by validation service to skip double validation")
    protected String validationToken;

    public String getValidationToken() {
        return validationToken;
    }

    public void setValidationToken(String validationToken) {
        this.validationToken = validationToken;
    }

}

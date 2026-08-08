package org.opensilex.core.csv.api;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * @author rcolin
 */
public class CsvImportDTO {

    @Schema(description = "Validation token provided by validation service to skip double validation")
    protected String validationToken;

    public String getValidationToken() {
        return validationToken;
    }

    public void setValidationToken(String validationToken) {
        this.validationToken = validationToken;
    }

}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.scientificObject.api;

import io.swagger.annotations.ApiModelProperty;
import java.net.URI;
import org.opensilex.server.rest.validation.ValidURI;

/**
 *
 * @author vmigot
 */
public class ScientificObjectCsvDescriptionDTO {

    @ValidURI
    @ApiModelProperty(value = "Scientific object experiment URI")
    protected URI experiment;

    @ValidURI
    @ApiModelProperty(value = "Validation token provided by validation service to skip double validation")
    protected String validationToken;

    public URI getExperiment() {
        return experiment;
    }

    public void setExperiment(URI experiment) {
        this.experiment = experiment;
    }

    public String getValidationToken() {
        return validationToken;
    }

    public void setValidationToken(String validationToken) {
        this.validationToken = validationToken;
    }

}

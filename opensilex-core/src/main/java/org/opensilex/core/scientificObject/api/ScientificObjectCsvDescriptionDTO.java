/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.scientificObject.api;

import java.net.URI;
import javax.validation.constraints.NotNull;

/**
 *
 * @author vmigot
 */
public class ScientificObjectCsvDescriptionDTO {

    @NotNull
    private URI context;

    @NotNull
    private URI type;

    private String validationToken;

    public URI getContext() {
        return context;
    }

    public void setContext(URI context) {
        this.context = context;
    }

    public URI getType() {
        return type;
    }

    public void setType(URI type) {
        this.type = type;
    }

    public String getValidationToken() {
        return validationToken;
    }

    public void setValidationToken(String validationToken) {
        this.validationToken = validationToken;
    }

}

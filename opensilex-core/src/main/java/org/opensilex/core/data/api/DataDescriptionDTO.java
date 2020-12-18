/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.data.api;

import java.net.URI;
import javax.validation.constraints.NotNull;

/**
 *
 * @author charlero
 */
public class DataDescriptionDTO {

    @NotNull
    private URI experiment; 
    
    @NotNull
    private URI provenance; 

//    private String validationToken;
    
    public URI getExperiment() {
        return experiment;
    }

    public void setExperiment(URI experiment) {
        this.experiment = experiment;
    }

    public URI getProvenance() {
        return provenance;
    }

    public void setProvenance(URI provenance) {
        this.provenance = provenance;
    }

    

//    public String getValidationToken() {
//        return validationToken;
//    }
//
//    public void setValidationToken(String validationToken) {
//        this.validationToken = validationToken;
//    }
    
}

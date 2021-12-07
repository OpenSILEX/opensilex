/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.ontology.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.opensilex.core.csv.dal.error.CSVValidationModel;

/**
 *
 * @author vmigot
 */
public class CSVValidationDTO {

    @JsonProperty("validation_token")
    private String validationToken;

    private CSVValidationModel errors;

    @JsonProperty("nb_lines_imported")
    private Integer nbLinesImported = 0;
        
    public Integer getNbLinesImported() {
        return nbLinesImported;
    }

    public void setNbLinesImported(Integer nbLinesImported) {
        this.nbLinesImported = nbLinesImported;
    }
    
    public String getValidationToken() {
        return validationToken;
    }

    public void setValidationToken(String validationToken) {
        this.validationToken = validationToken;
    }

    public CSVValidationModel getErrors() {
        return errors;
    }

    public void setErrors(CSVValidationModel errors) {
        this.errors = errors;
    }

}

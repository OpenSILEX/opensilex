/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.data.api;

import org.opensilex.core.csv.api.CSVValidationDTO;
import org.opensilex.core.data.dal.DataCSVValidationModel;

/**
 *
 * @author charlero
 */
public class DataCSVValidationDTO extends CSVValidationDTO {

    private DataCSVValidationModel dataErrors;
    
    public int sizeMax = DataAPI.SIZE_MAX;

    public DataCSVValidationModel getDataErrors() {
        return dataErrors;
    }

    public void setDataErrors(DataCSVValidationModel dataErrors) {
        this.dataErrors = dataErrors;
    }

}

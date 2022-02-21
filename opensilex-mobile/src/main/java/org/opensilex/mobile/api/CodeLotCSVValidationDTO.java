//******************************************************************************
//                          CodeLotCSVValidationDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2021
// Contact: maximilian.hart@inrae.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.mobile.api;

import org.opensilex.core.ontology.api.CSVValidationDTO;
import org.opensilex.mobile.dal.CodeLotCSVValidationModel;

/**
 * This class is the Data Transfer Object containing any errors that were produced during the Code lot csv import.
 *
 * @author Maximilian Hart
 */
public class CodeLotCSVValidationDTO extends CSVValidationDTO {

    private CodeLotCSVValidationModel codelotErrors;
    

    public CodeLotCSVValidationModel getCodelotErrors() {
        return codelotErrors;
    }

    public void setCodelotErrors(CodeLotCSVValidationModel codelotErrors) {
        this.codelotErrors = codelotErrors;
    }

}

//******************************************************************************
//                        ProvenanceDateValidator.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 21, Jun 2018
// Contact: arnaud.charleroy@inra.fr, morgane.vidal@inra.fr, anne.tireau@inra.fr, 
// pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.resources.validation.validators;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import org.joda.time.DateTime;
import phis2ws.service.configuration.DateFormat;
import phis2ws.service.resources.dto.ProvenanceDTO;
import phis2ws.service.resources.validation.interfaces.ProvenanceDate;
import phis2ws.service.utils.dates.Dates;

/**
 * Class checks specific date field on provenance object.
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>, Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ProvenanceDateValidator implements ConstraintValidator<ProvenanceDate, ProvenanceDTO> {

    private DateFormat dateFormat;

    @Override
    public void initialize(ProvenanceDate constraintAnnotation) {
        this.dateFormat = constraintAnnotation.value();
    }

    @Override
    public boolean isValid(ProvenanceDTO provenance, ConstraintValidatorContext context) {
        if (provenance == null) {
            return true;
        }
        if(provenance.getUri() != null){
             return true;
        }
        // if uri is valid and date not return false
        return  provenance.getUri() == null && validateDate(dateFormat, provenance.getCreationDate());
    }

    public static boolean validateDate(DateFormat pattern, String date) {
        if(date == null){
            return false;
        }
        
        DateTime stringToDateTime = Dates.stringToDateTimeWithGivenPattern(date, pattern.toString());
        if (stringToDateTime == null) {
            return false;
        }
        return true;
    }

}

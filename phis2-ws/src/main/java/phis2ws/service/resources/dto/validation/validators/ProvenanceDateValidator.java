//******************************************************************************
//                                       ProvenanceDateValidator.java
//
// Author(s): Arnaud Charleroy <arnaud.charleroy@inra.fr>
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: 21 juin 2018
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  21 juin 2018
// Subject: Class checks specific date field on provenance object
//******************************************************************************
package phis2ws.service.resources.dto.validation.validators;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import org.joda.time.DateTime;
import phis2ws.service.configuration.DateFormat;
import phis2ws.service.resources.dto.ProvenanceDTO;
import phis2ws.service.resources.dto.validation.interfaces.Date;
import phis2ws.service.resources.dto.validation.interfaces.ProvenanceDate;
import phis2ws.service.utils.dates.Dates;

/**
 *
 * @author Arnaud Charleroy<arnaud.charleroy@inra.fr>
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

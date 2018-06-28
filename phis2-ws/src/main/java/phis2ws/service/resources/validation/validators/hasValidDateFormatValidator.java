//******************************************************************************
//                                       hasValidDateFormatValidator.java
//
// Author(s): Arnaud Charleroy <arnaud.charleroy@inra.fr>
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: 25 juin 2018
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  25 juin 2018
// Subject: Class used by hasValidDateFormat annotation to validate a String value 
//******************************************************************************
package phis2ws.service.resources.validation.validators;

/**
 *
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>
 */
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import org.joda.time.DateTime;
import phis2ws.service.resources.validation.interfaces.IsValidURI;
import phis2ws.service.utils.dates.Dates;

public class hasValidDateFormatValidator implements ConstraintValidator<IsValidURI, String> {

    @Override
    public void initialize(IsValidURI constraintAnnotation) {
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        return validateDate(value);
    }

    public static boolean validateDate(String date) {
        DateTime stringToDateTime = Dates.stringToDateTime(date);
        if(stringToDateTime == null ){
             return false;
        }

        return true;
    }
}

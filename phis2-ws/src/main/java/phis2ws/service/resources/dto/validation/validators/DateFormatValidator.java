//******************************************************************************
//                                       DateFormatValidator.java
//
// Author(s): Arnaud Charleroy, Morgane Vidal
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: 29 juin 2018
// Contact: arnaud.charleroy@inra.fr, morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  29 juin 2018
// Subject: Class used by DateFormat annotation to validate a string value with a specific date format 
//******************************************************************************
package phis2ws.service.resources.dto.validation.validators;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import org.joda.time.DateTime;
import phis2ws.service.configuration.DateFormat;
import phis2ws.service.utils.dates.Dates;
import phis2ws.service.resources.dto.validation.interfaces.Date;

/**
 * Class used by DateFormat annotation to validate a string value with a
 * specific date format
 *
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>
 */
public class DateFormatValidator implements ConstraintValidator<Date, String> {

    private DateFormat dateFormat;

    @Override
    public void initialize(Date constraintAnnotation) {
        this.dateFormat = constraintAnnotation.value();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        return validateDate(dateFormat, value);
    }

    public static boolean validateDate(DateFormat pattern, String date) {
        DateTime stringToDateTime = Dates.stringToDateTimeWithGivenPattern(date, pattern.toString());
        if (stringToDateTime == null) {
            return false;
        }
        return true;
    }
}

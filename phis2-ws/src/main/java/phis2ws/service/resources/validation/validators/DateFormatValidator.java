//******************************************************************************
//                                       DateFormatValidator.java
//
// SILEX-PHIS
// Copyright © INRA 2018
// Contact: arnaud.charleroy@inra.fr, morgane.vidal@inra.fr, anne.tireau@inra.fr, 
// pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.resources.validation.validators;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import org.joda.time.DateTime;
import phis2ws.service.configuration.DateFormat;
import phis2ws.service.utils.dates.Dates;
import phis2ws.service.resources.validation.interfaces.Date;

/**
 * Class used by DateFormat annotation to validate a string value with a
 * specific date format
 * @see Date
 * @see DateFormat
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

    public boolean validateDate(DateFormat pattern, String date) {
        DateTime stringToDateTime = Dates.stringToDateTimeWithGivenPattern(date, pattern.toString());
        return stringToDateTime != null;
    }
}

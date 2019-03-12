//******************************************************************************
//                                       DateFormatValidator.java
//
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
import phis2ws.service.utils.dates.Dates;
import phis2ws.service.resources.validation.interfaces.Date;

/**
 * Class used by DateFormat annotation to validate a string value with one
 * or more date format, the value is considered valid as soon as it correspond to one format.
 * {@code null} elements are considered valid.
 * @see Date
 * @see DateFormat
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>, Morgane Vidal <morgane.vidal@inra.fr>
 */
public class DateFormatValidator implements ConstraintValidator<Date, String> {

    private DateFormat[] dateFormat;

    @Override
    public void initialize(Date constraintAnnotation) {
        this.dateFormat = constraintAnnotation.value();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        boolean isValid = false;
        for (DateFormat dateCheckFormat : dateFormat) {
            if (validateDate(dateCheckFormat, value)) {
                isValid = true;
                break;
            }
        }
        
        return isValid;
    }

    public boolean validateDate(DateFormat pattern, String date) {
        DateTime stringToDateTime = Dates.stringToDateTimeWithGivenPattern(date, pattern.toString());
        return stringToDateTime != null;
    }
}

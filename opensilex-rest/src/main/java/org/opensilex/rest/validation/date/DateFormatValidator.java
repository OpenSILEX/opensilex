package org.opensilex.rest.validation.date;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 *
 */
public class DateFormatValidator implements ConstraintValidator<DateConstraint, String> {

    protected DateTimeFormatter format;

    @Override
    public void initialize(DateConstraint dateConstraint) {
        format = DateTimeFormatter.ofPattern(dateConstraint.value());
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        try {
            format.parse(value);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }
}
//******************************************************************************
//                      APIExtension.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.server.rest.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Validator for dates constraints.
 *
 * @author Vincent Migot
 */
public class DateFormatValidator implements ConstraintValidator<DateConstraint, String> {

    /**
     * Date formater.
     */
    private DateTimeFormatter format;

    /**
     * Initialize date formater.
     *
     * @param dateConstraint
     */
    @Override
    public void initialize(DateConstraint dateConstraint) {
        format = DateTimeFormatter.ofPattern(dateConstraint.value());
    }

    /**
     * Check if value is a valid date according to defined formatter.
     *
     * @param value value to check
     * @param context validation context.
     * @return true if value is a valid date or false otherwise
     */
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

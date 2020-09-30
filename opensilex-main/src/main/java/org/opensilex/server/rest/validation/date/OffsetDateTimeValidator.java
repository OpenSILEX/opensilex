//******************************************************************************
//                      OffsetDateTimeValidator.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************

package org.opensilex.server.rest.validation.date;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

/**
 *  @author Renaud COLIN
 *   This validator theck that a {@link String} could be parsed as an {@link java.time.OffsetDateTime}.
 */
public class OffsetDateTimeValidator implements ConstraintValidator<ValidOffsetDateTime, String> {

    /**
     * @return true is value could be parsed as an {@link java.time.OffsetDateTime}, false else
     */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        try {
            if (value == null) {
                return true;
            }
            OffsetDateTime.parse(value);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }
}

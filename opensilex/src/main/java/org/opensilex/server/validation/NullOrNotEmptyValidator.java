package org.opensilex.server.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;


public class NullOrNotEmptyValidator implements ConstraintValidator<NullOrNotEmpty, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        return !value.isEmpty();
    }

}

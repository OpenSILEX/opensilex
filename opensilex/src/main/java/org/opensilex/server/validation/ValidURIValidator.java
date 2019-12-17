package org.opensilex.server.validation;

import java.net.MalformedURLException;
import java.net.URI;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class ValidURIValidator implements ConstraintValidator<ValidURI, URI> {

    @Override
    public boolean isValid(URI value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        return value.isAbsolute();
    }
}

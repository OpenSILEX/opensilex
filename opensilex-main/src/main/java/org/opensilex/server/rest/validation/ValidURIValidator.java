//******************************************************************************
//                      APIExtension.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.server.rest.validation;

import java.net.URI;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * Validator for URI parameters.
 *
 * @author Vincent Migot
 */
public class ValidURIValidator implements ConstraintValidator<ValidURI, URI> {

    /**
     * Validate an URI.
     *
     * @param value
     * @param context
     * @return
     */
    @Override
    public boolean isValid(URI value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        if (value.isAbsolute()) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(value + " is not a valid URI")
                .addConstraintViolation();

        return false;
    }
}

//******************************************************************************
//                         URLListValidator.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2018
// Contact: arnaud.charleroy@inra.fr, morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.server.rest.validation;

import java.net.URI;
import java.util.List;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * Class used by URL annotation to validate that a string value list contains an
 * URL. {@code null} elements are considered valid.
 *
 * @see org.opensilex.server.rest.validation.URL
 * @author Arnaud Charleroy
 * @author Morgane Vidal
 */
public class ValidURIListValidator implements ConstraintValidator<ValidURI, List<URI>> {

    /**
     * Check is list is made of valid URL.
     *
     * @param valueList list of values to check
     * @param context constraint context to strore errors
     * @return true if all values are URL and false otherwise
     */
    @Override
    public boolean isValid(List<URI> valueList, ConstraintValidatorContext context) {
        if (valueList == null) {
            return true;
        }

        Boolean allValid = true;
        URI lastUrlCheck = null;

        // loop over string
        for (URI uri : valueList) {
            if (!uri.isAbsolute()) {
                allValid = false;
                lastUrlCheck = uri;
            }
        }

        // if not valid returns the last false index
        if (!allValid) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "The uri at the index [" + valueList.indexOf(lastUrlCheck) + "] is not an URL"
            ).addConstraintViolation();
        }
        return allValid;
    }
}

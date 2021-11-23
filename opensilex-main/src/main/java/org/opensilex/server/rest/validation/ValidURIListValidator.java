//******************************************************************************
//                         URLListValidator.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2018
// Contact: arnaud.charleroy@inra.fr, morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.server.rest.validation;

import java.net.URI;
import java.util.LinkedList;
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

    private static final String INVALID_URI_MSG = "The URI at the index [%d] is not an URL : %s";

    /**
     * Check is list is made of valid URL.
     *
     * @param valueList list of values to check
     * @param context constraint context to store errors
     * @return true if all values are URL and false otherwise
     */
    @Override
    public boolean isValid(List<URI> valueList, ConstraintValidatorContext context) {
        if (valueList == null) {
            return true;
        }

        boolean allValid = true;
        List<Integer> invalidUrisIndexes = new LinkedList<>();

        for (int i = 0; i < valueList.size(); i++) {
            URI uri = valueList.get(i);
            if (uri != null && !uri.isAbsolute()) {
                allValid = false;
                invalidUrisIndexes.add(i);
            }
        }

        // append a custom constraint violation for each bad URI
        if (!allValid) {
            context.disableDefaultConstraintViolation();

            invalidUrisIndexes.forEach(badUriIdx -> {
                // start from index 1 instead of 0 for better error comprehension for any user
                String msg = String.format(INVALID_URI_MSG, badUriIdx + 1, valueList.get(badUriIdx));
                context.buildConstraintViolationWithTemplate(msg).addConstraintViolation();
            });

        }
        return allValid;
    }
}

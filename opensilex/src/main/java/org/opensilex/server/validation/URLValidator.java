//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2018
// Contact: arnaud.charleroy@inra.fr, morgane.vidal@inra.fr, 
// anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.server.validation;

import java.net.MalformedURLException;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * Class used by URL annotation to validate that a string value is an URL.
 * {@code null} elements are considered valid.
 * @see  org.opensilex.server.validation.URL
 * @author Arnaud Charleroy
 * @author Morgane Vidal
 */
public class URLValidator implements ConstraintValidator<URL, String> {

    @Override
    public void initialize(URL constraintAnnotation) {
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        return validateURL(value);
    }

    public static boolean validateURL(String url) {
        try {
        	new java.net.URL(url);
        } catch (MalformedURLException e1) {
            return false;
        }
        return true;
    }
}
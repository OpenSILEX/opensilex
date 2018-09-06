
//******************************************************************************
//                                       URLValidator.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 21, Jun 2018
// Contact: arnaud.charleroy@inra.fr, morgane.vidal@inra.fr, anne.tireau@inra.fr, 
// pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.resources.validation.validators;

import java.net.MalformedURLException;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import phis2ws.service.resources.validation.interfaces.URL;

/**
 * Class used by URL annotation to validate that a string value is an URL.
 * {@code null} elements are considered valid.
 * @see  phis2ws.service.resources.validation.interfaces.URL
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>, Morgane Vidal <morgane.vidal@inra.fr>
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
        final java.net.URL finalUrl;
        try {
            finalUrl = new java.net.URL(url);
        } catch (MalformedURLException e1) {
            return false;
        }
        return true;
    }
}
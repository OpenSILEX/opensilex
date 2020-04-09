//******************************************************************************
//                             URLValidator.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 21 June 2018
// Contact: arnaud.charleroy@inra.fr, morgane.vidal@inra.fr, anne.tireau@inra.fr, 
//          pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.validation.validator;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import opensilex.service.resource.validation.interfaces.URL;

/**
 * Used by URL annotation to validate that a string value is an URL.
 * {@code null} elements are considered valid.
 * @see  opensilex.service.resource.validation.interfaces.URL
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
        final URI finalUrl;
        try {
            finalUrl = new URI(url);
        } catch (URISyntaxException ex) {
             return false;
        }
        return true;
    }
}
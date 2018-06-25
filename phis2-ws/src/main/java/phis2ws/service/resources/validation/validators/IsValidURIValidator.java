//******************************************************************************
//                                       IsValidURIValidator.java
//
// Author(s): Arnaud Charleroy <arnaud.charleroy@inra.fr>
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: 25 juin 2018
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  25 juin 2018
// Subject: Class used by IsValidURI annotation to validate a String value 
//******************************************************************************
package phis2ws.service.resources.validation.validators;

/**
 *
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>
 */
import java.net.MalformedURLException;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.net.URL;
import phis2ws.service.resources.validation.interfaces.IsValidURI;

public class IsValidURIValidator implements ConstraintValidator<IsValidURI, String> {

    @Override
    public void initialize(IsValidURI constraintAnnotation) {
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        return validateURI(value);
    }

    public static boolean validateURI(String uri) {
        final URL url;
        try {
            url = new URL(uri);
        } catch (MalformedURLException e1) {
            return false;
        }
        return true;
    }
}

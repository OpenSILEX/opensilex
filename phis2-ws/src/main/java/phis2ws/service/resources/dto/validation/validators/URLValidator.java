
//******************************************************************************
//                                       URLValidator.java
//
// Author(s): Arnaud Charleroy, Morgane Vidal
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: 28 juin 2018
// Contact: arnaud.charleroy@inra.fr, morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  28 juin 2018
// Subject: Class used by URL annotation to validate that a string value is an URL
//******************************************************************************
package phis2ws.service.resources.dto.validation.validators;

import java.net.MalformedURLException;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import phis2ws.service.resources.dto.validation.interfaces.URL;

/**
 * Class used by URL annotation to validate that a string value is an URL
 * @see  phis2ws.service.resources.dto.validation.interfaces.URL
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>
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
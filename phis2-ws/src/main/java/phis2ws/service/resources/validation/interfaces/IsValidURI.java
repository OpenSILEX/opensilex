//******************************************************************************
//                                       IsValidURI.java
//
// Author(s): Arnaud Charleroy <arnaud.charleroy@inra.fr>
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: 25 juin 2018
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  25 juin 2018
// Subject: Use to specify if a string is a valid URI
//******************************************************************************
package phis2ws.service.resources.validation.interfaces;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import static java.lang.annotation.ElementType.PARAMETER;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;
import phis2ws.service.resources.validation.validators.IsValidURIValidator;

/**
 *
 * @author Arnaud Charleroy<arnaud.charleroy@inra.fr>
 */

@Target({ METHOD, FIELD, PARAMETER })
@Retention(RUNTIME)
@Constraint(validatedBy = IsValidURIValidator.class)
public @interface IsValidURI {
 
    String message() default "This string is not an URI";
 
    Class<?>[] groups() default {};
 
    Class<? extends Payload>[] payload() default {};
}

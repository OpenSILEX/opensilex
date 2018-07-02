//******************************************************************************
//                                       Date.java
//
// Author(s): Arnaud Charleroy <arnaud.charleroy@inra.fr>
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: 29 juin 2018
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  29 juin 2018
// Subject: Use to specify if a string is has a valid date format
//******************************************************************************
package phis2ws.service.resources.dto.validation.interfaces;

import java.lang.annotation.Documented;
import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import static java.lang.annotation.ElementType.PARAMETER;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;
import phis2ws.service.configuration.DateFormat;
import phis2ws.service.resources.dto.validation.validators.DateFormatValidator;

/**
 * Use to specify if a string is has a valid date format
 * @author Arnaud Charleroy<arnaud.charleroy@inra.fr>
 */

@Target({ METHOD, FIELD, PARAMETER })
@Retention(RUNTIME)
@Constraint(validatedBy = DateFormatValidator.class)
public @interface Date {
 
    String message() default "is not a valid date. Excepted format : {value}";
 
    Class<?>[] groups() default {};
 
    Class<? extends Payload>[] payload() default {};
    
    DateFormat value() default DateFormat.YMDHMSZ;
    
}
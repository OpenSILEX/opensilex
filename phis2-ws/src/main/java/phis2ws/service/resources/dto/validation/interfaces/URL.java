//******************************************************************************
//                                       URL.java
//
// Author(s): Arnaud Charleroy, Morgane Vidal
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: 28 juin 2018
// Contact: arnaud.charleroy@inra.fr, morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  28 juin 2018
// Subject: Annotation used to check if a string is a valid URL
//******************************************************************************
package phis2ws.service.resources.dto.validation.interfaces;

import java.lang.annotation.Documented;
import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import static java.lang.annotation.ElementType.PARAMETER;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;
import phis2ws.service.resources.dto.validation.validators.URLListValidator;
import phis2ws.service.resources.dto.validation.validators.URLValidator;

/**
 * Annotation used to check if a string is a valid URL
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>
 */

@Target(value={METHOD,FIELD,ANNOTATION_TYPE,CONSTRUCTOR,PARAMETER})
@Retention(RUNTIME)
@Constraint(validatedBy = {URLValidator.class,URLListValidator.class})
public @interface URL {
 
    String message() default "is not an URL";
 
    Class<?>[] groups() default {};
 
    Class<? extends Payload>[] payload() default {};
    
    @Target(value = {METHOD,FIELD,ANNOTATION_TYPE,CONSTRUCTOR,PARAMETER})
    @Retention(value =RUNTIME)
    @Documented
    public @interface List {
        public URL[] value();
    }

}

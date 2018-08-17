//******************************************************************************
//                                       OType.java
// SILEX-PHIS
// Copyright © INRA 2018
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.resources.dto.validation.interfaces;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;
import phis2ws.service.resources.dto.validation.validators.OTypeValidator;

/**
 * Interface to check that the given o type is one of thoses expected
 * @see OTypeValidator
 * @see https://www.w3.org/wiki/JSON_Triple_Sets
 * @author Arnaud Charleroy<arnaud.charleroy@inra.fr>, Morgane Vidal <morgane.vidal@inra.fr>
 */
@Target(value={METHOD,FIELD,ANNOTATION_TYPE,CONSTRUCTOR,PARAMETER})
@Retention(RUNTIME)
@Constraint(validatedBy = OTypeValidator.class)
public @interface OType {
    String message() default "is not a valid o type";
 
    Class<?>[] groups() default {};
 
    Class<? extends Payload>[] payload() default {};
}

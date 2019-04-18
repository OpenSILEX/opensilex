//******************************************************************************
//                               SortingValue.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 7 Sept 2018
// Contact: arnaud.charleroy@inra.fr, morgane.vidal@inra.fr, anne.tireau@inra.fr, 
//          pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.validation.interfaces;

import java.lang.annotation.Documented;
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
import opensilex.service.resource.validation.validator.SortingValueValidator;

/**
 * Interface to check that the given sorting value is one of those expected.
 * @see SortingValueValidator
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>, Morgane Vidal <morgane.vidal@inra.fr>
 */
@Target(value={METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER})
@Retention(RUNTIME)
@Constraint(validatedBy = SortingValueValidator.class)
@Documented
public @interface SortingValue {
    String message() default "is not a valid sort value, 'asc' or 'desc' is expected";
 
    Class<?>[] groups() default {};
 
    Class<? extends Payload>[] payload() default {};
}
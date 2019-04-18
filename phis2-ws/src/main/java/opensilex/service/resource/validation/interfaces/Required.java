//******************************************************************************
//                                 Required.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 25 June 2018
// Contact: arnaud.charleroy@inra.fr, morgane.vidal@inra.fr, anne.tireau@inra.fr, 
//          pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.validation.interfaces;

/**
 * Multiple annotations which provides the @required annotation.
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>
 */
import java.lang.annotation.Documented;
import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.ElementType.PARAMETER;

import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;
import javax.validation.ReportAsSingleViolation;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * Annotation that verify if a field is required.
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>, Morgane Vidal <morgane.vidal@inra.fr>
 */
@NotNull
@NotBlank
@NotEmpty
@Target({METHOD, FIELD, ANNOTATION_TYPE, TYPE, PARAMETER})
@Retention(RUNTIME)
@Constraint(validatedBy = {})
@Documented
@ReportAsSingleViolation
public @interface Required {
    String message() default "is required and must be filled";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

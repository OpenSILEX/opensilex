//******************************************************************************
//                              Required.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2018
// Contact: arnaud.charleroy@inra.fr, morgane.vidal@inra.fr, 
// anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.server.validation;

import java.lang.annotation.Documented;
import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
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
 *
 * @author Arnaud Charleroy
 * @author Morgane Vidal
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

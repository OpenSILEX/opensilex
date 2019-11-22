//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2018
// Contact: arnaud.charleroy@inra.fr, morgane.vidal@inra.fr, 
// anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.server.validation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;
import java.lang.annotation.*;
import javax.validation.*;
import javax.validation.constraints.*;


/**
 * Annotation that verify if a field is required.
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

//******************************************************************************
//                      APIExtension.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.server.rest.validation;

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

/**
 * Validation for URI.
 *
 * @author Vincent Migot
 */
@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER})
@Retention(RUNTIME)
@Constraint(validatedBy = {ValidURIValidator.class})
@Documented
public @interface ValidURI {

    /**
     * Validation error message for URI.
     *
     * @return validation error message for URI
     */
    String message() default "is not an URI";

    /**
     * Validation group.
     *
     * @return Validation group
     */
    Class<?>[] groups() default {};

    /**
     * Validation payload.
     *
     * @return Validation payload
     */
    Class<? extends Payload>[] payload() default {};
}

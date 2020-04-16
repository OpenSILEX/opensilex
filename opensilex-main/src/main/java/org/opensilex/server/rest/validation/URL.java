//******************************************************************************
//                          URL.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2018
// Contact: arnaud.charleroy@inra.fr, morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
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
 * Annotation used to check if a string is a valid URL.
 *
 * @author Arnaud Charleroy
 */
@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER})
@Retention(RUNTIME)
@Constraint(validatedBy = {URLValidator.class, URLListValidator.class})
@Documented
public @interface URL {

    /**
     * Error message.
     *
     * @return error message.
     */
    String message() default "is not an URL";

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

    /**
     * Apply this interface on list of elements.
     */
    @Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER})
    @Retention(RUNTIME)
    @Documented
    public @interface List {

        /**
         * Return list or URLs.
         *
         * @return list or URLs
         */
        public URL[] value();
    }
}

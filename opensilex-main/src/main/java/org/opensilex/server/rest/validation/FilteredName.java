//******************************************************************************
//                          FilteredName.java
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
 * Annotation used to check if a name contians bad URL caracters for URI generation
 * 
 * RFC 1738 specification:
 * Thus, only alphanumerics, the special characters "$-_.+!*'(),", and reserved characters used for their reserved purposes may be used unencoded within a URL.
 *
 * @author Arnaud Charleroy
 */
@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER})
@Retention(RUNTIME)
@Constraint(validatedBy = {FilteredNameValidator.class })
@Documented
public @interface FilteredName {

    /**
     * Error message.
     *
     * @return error message.
     */
    String message() default " must not contains : -,+,=,<,>,=,?,/,*,&";

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

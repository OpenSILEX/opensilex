//******************************************************************************
//                      ValidOffsetDateTime.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************

package org.opensilex.server.rest.validation.date;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;


/**
 * @author Renaud COLIN
 *
 * Validation annotation for checking if a {@link String} is parsable as an {@link java.time.OffsetDateTime}
 */
@Target({METHOD, FIELD, PARAMETER})
@Retention(RUNTIME)
@Constraint(validatedBy = OffsetDateTimeValidator.class)
public @interface ValidOffsetDateTime {

    String message() default "is not a valid OffsetDateTime. Excepted format : yyyy-mm-ddThh:mm:ss-mm:hh";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}

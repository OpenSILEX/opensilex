package org.opensilex.rest.validation.date;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;

/**
 * Date validator.
 * Used to specify if a string has a valid date format.
 * @see DateFormatValidator
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>, Morgane Vidal <morgane.vidal@inra.fr>
 */
@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER})
@Retention(RUNTIME)
@Constraint(validatedBy = DateFormatValidator.class)
public @interface DateConstraint {

    String message() default "is not a valid date. Excepted format : {value}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    String value() default "yyyy-MM-dd";

}
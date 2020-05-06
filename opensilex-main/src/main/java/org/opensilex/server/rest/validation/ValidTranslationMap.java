/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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
import org.opensilex.OpenSilex;

/**
 *
 * @author charlero
 */
@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER})
@Retention(RUNTIME)
@Constraint(validatedBy = {ValidTranslationMapValidator.class})
@Documented
public @interface ValidTranslationMap {

      /**
     * Validation error message.
     *
     * @return error message for invalid date
     */
    String message() default "is not a valid translated label at least  english name is required names : {\"en\":\"englishLabel\"} expected";

    /**
     * Date pattern value.
     *
     * @return date pattern
     */
    String languageValue() default OpenSilex.DEFAULT_LANGUAGE;

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

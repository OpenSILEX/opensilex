//******************************************************************************
//                      APIExtension.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: gabriel.besombes@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.server.rest.validation;

import org.opensilex.OpenSilexModule;
import org.opensilex.server.rest.validation.model.OpenSilexLocale;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Validation for Language. Given an {@link OpenSilexModule}, a config class and a first level key for an entry in the
 * config validate whether an {@link OpenSilexLocale} matches with a language in the list of available languages in the
 * config under the key.
 *
 * @author Gabriel Besombes
 */
@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER})
@Retention(RUNTIME)
@Constraint(validatedBy = {ValidLanguageValidator.class})
@Documented
public @interface ValidLanguage {

    Class<? extends OpenSilexModule> moduleClass();
    Class<?> configClass();
    String configKey();

    /**
     * Validation error message for Language.
     *
     * @return validation error message for Language
     */
    String message() default "Unavailable language";

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

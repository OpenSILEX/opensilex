//******************************************************************************
//                      APIExtension.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: gabriel.besombes@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.server.rest.validation;

import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;
import org.opensilex.OpenSilex;
import org.opensilex.OpenSilexModule;
import org.opensilex.OpenSilexModuleNotFoundException;
import org.opensilex.server.rest.validation.model.OpenSilexLocale;

import javax.inject.Inject;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Validator for Languages parameters.
 *
 * @author Gabriel Besombes
 */
public class ValidLanguageValidator implements ConstraintValidator<ValidLanguage, OpenSilexLocale> {

    @Inject
    private OpenSilex openSilex;

    private Class<? extends OpenSilexModule> moduleClass;
    private Class<?> configClass;
    private String configKey;

    @Override
    public void initialize(ValidLanguage constraintAnnotation) {
        this.moduleClass = constraintAnnotation.moduleClass();
        this.configClass = constraintAnnotation.configClass();
        this.configKey = constraintAnnotation.configKey();
    }

    /**
     * Validate a Language.
     *
     * @param value the language to check
     * @param context the ConstraintValidatorContext to pass the error message parameters to
     * @return a boolean representing whether "value" is a valid language or not
     */
    @Override
    public boolean isValid(OpenSilexLocale value, ConstraintValidatorContext context) {

        // NOTE : this works but not ideal. Can only access keys on the first level of the config
        List<String> allowedLanguages;
        try {
            Method configMethodParameter = this.configClass.getMethod(this.configKey);
            Object configClass = this.openSilex.getModuleConfig(this.moduleClass, this.configClass);
            allowedLanguages = ((List<?>) configMethodParameter.invoke(configClass))
                    .stream().map(o -> (String) o)
                    .collect(Collectors.toList());

        } catch (NoSuchMethodException | OpenSilexModuleNotFoundException | InvocationTargetException |
                 IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        if (value == null || allowedLanguages.contains(value.toString())){
            return true;
        } else {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Value '" + value + "' is not part of the allowed values : " + allowedLanguages)
                    .addConstraintViolation();
            return false;
        }
    }
}

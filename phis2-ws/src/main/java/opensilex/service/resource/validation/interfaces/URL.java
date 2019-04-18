//******************************************************************************
//                                  URL.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 28 June 2018
// Contact: arnaud.charleroy@inra.fr, morgane.vidal@inra.fr, anne.tireau@inra.fr, 
//          pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.validation.interfaces;

import java.lang.annotation.Documented;
import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import static java.lang.annotation.ElementType.PARAMETER;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;
import opensilex.service.resource.validation.validator.URLListValidator;
import opensilex.service.resource.validation.validator.URLValidator;

/**
 * Annotation used to check if a string is a valid URL.
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>
 */
@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER})
@Retention(RUNTIME)
@Constraint(validatedBy = {URLValidator.class, URLListValidator.class})
@Documented
public @interface URL {
    String message() default "is not an URL";
 
    Class<?>[] groups() default {};
 
    Class<? extends Payload>[] payload() default {};
    
    /**
     * Apply this interface on list of elements
     */
    @Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER})
    @Retention(RUNTIME)
    @Documented
    public @interface List {
        public URL[] value();
    }
}

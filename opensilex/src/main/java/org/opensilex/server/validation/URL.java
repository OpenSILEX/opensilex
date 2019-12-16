//******************************************************************************
//                          URL.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2018
// Contact: arnaud.charleroy@inra.fr, morgane.vidal@inra.fr, 
// anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.server.validation;

import java.lang.annotation.*;
import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;
import javax.validation.*;

/**
 * Annotation used to check if a string is a valid URL
 *
 * @author Arnaud Charleroy
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

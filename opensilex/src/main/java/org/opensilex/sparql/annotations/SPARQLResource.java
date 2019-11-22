//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.annotations;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;
import java.lang.annotation.*;
import org.opensilex.sparql.utils.*;


/**
 *
 * @author vincent
 */
@Target(TYPE)
@Retention(RUNTIME)
@Documented
@Inherited
public @interface SPARQLResource {
    
    Class<?> ontology();
    String resource();
    Class<? extends URIGenerator> uriGenerator() default DefaultURIGenerator.class;
    String graph() default "";
}

//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.annotations;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;
import java.lang.annotation.*;


/**
 *
 * @author Vincent Migot
 */
@Target(FIELD)
@Retention(RUNTIME)
@Documented
@Inherited
public @interface SPARQLProperty {
    
    Class<?> ontology();
    
    String property();
    
    boolean required() default false;
    
    boolean inverse() default false;
}

//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.annotations;

import java.lang.annotation.Documented;
import static java.lang.annotation.ElementType.FIELD;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;



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
    
    boolean ignoreUpdateIfNull() default false;
    
    boolean cascadeDelete() default false;
    
    boolean autoUpdate() default false;
    
    boolean useDefaultGraph() default true;
}

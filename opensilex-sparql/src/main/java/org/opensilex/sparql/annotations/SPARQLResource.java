//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.annotations;

import org.opensilex.uri.generation.DefaultURIGenerator;
import org.opensilex.uri.generation.URIGenerator;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
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

    String prefix() default "";

    boolean ignoreValidation() default false;

    boolean allowBlankNode() default false;
}

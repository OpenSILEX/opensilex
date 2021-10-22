//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.annotations;

import java.lang.annotation.Documented;

import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Target;

import org.opensilex.sparql.utils.DefaultURIGenerator;
import org.opensilex.sparql.utils.URIGenerator;

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

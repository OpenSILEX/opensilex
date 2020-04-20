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
 *
 * @author vincent
 */
@Target(TYPE)
@Retention(RUNTIME)
@Documented
@Inherited
public @interface SPARQLResource {

    public Class<?> ontology();

    public String resource();

    public Class<? extends URIGenerator> uriGenerator() default DefaultURIGenerator.class;

    public String graph() default "";

    public String prefix() default "";

    public boolean ignoreValidation() default false;

    public boolean allowBlankNode() default false;
}

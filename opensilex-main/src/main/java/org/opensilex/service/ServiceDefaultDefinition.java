//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.service;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for describing service default configuration.
 *
 * @author Vincent Migot
 */
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = ElementType.TYPE)
@Documented
public @interface ServiceDefaultDefinition {

    /**
     * Service implementation class.
     *
     * @return implementation class
     */
    public Class<? extends Service> implementation() default Service.class;

    /**
     * Service configuration class.
     *
     * @return configuration class
     */
    public Class<?> configClass() default Class.class;

    /**
     * Service configuration ID.
     *
     * @return configuration ID
     */
    public String configID() default "";

    /**
     * Service constructor dependency ID.
     *
     * @return constructor dependency ID
     */
    public String serviceID() default "";

    /**
     * Service constructor dependency class.
     *
     * @return constructor dependency class
     */
    public Class<? extends Service> serviceClass() default Service.class;

}

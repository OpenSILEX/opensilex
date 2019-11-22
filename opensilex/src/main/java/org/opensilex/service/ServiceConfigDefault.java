//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.service;

import java.lang.annotation.*;

/**
 *
 * @author Vincent Migot
 */
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = ElementType.TYPE)
@Documented
public @interface ServiceConfigDefault {

    public Class<? extends Service> implementation() default Service.class;

    public Class<?> configClass() default Class.class;
    
    public String configID() default "";
    
    public Class<?> connectionConfig() default Class.class;

    public String connectionConfigID() default "";

    public Class<? extends ServiceConnection> connection() default ServiceConnection.class;

}

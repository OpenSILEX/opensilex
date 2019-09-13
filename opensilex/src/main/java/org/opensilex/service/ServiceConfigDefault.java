/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.service;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.opensilex.service.Service;
import org.opensilex.service.ServiceConnection;

/**
 *
 * @author vincent
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

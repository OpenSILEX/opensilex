/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * @author vincent
 */
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
@Documented
public @interface ServiceDescription {

    public String value();

    public Class<?> connection() default Class.class;

    public Class<?> defaultConnection() default Class.class;

    public Class<?> defaultConnectionConfig() default Class.class;

    public String defaultConnectionConfigID() default "";

}

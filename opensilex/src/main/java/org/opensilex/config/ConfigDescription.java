/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.config;

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
public @interface ConfigDescription {

    String value();

    boolean defaultBoolean() default false;

    byte defaultByte() default (byte) 0;

    char defaultChar() default Character.MIN_VALUE;

    double defaultDouble() default 0d;

    float defaultFloat() default 0f;

    int defaultInt() default 0;

    long defaultLong() default 0L;

    short defaultShort() default (short) 0;

    String defaultString() default "";

    String[] defaultList() default {};

    String[] defaultMap() default {};

    Class<?> defaultClass() default Class.class;    
}

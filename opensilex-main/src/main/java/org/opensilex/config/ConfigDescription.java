//******************************************************************************
//                      ConfigDescription.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for configuration description and default values
 *
 * @author Vincent Migot
 */
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
@Documented
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

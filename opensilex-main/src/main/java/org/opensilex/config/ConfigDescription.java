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
 * Annotation for configuration description and default values.
 *
 * @author Vincent Migot
 */
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
@Documented
public @interface ConfigDescription {

    /**
     * Configuration description content.
     *
     * @return Configuration description content
     */
    String value();

    /**
     * Default boolean value.
     *
     * @return Default boolean value
     */
    boolean defaultBoolean() default false;

    /**
     * Default byte value.
     *
     * @return Default byte value
     */
    byte defaultByte() default (byte) 0;

    /**
     * Default char value.
     *
     * @return Default char value
     */
    char defaultChar() default Character.MIN_VALUE;

    /**
     * Default double value.
     *
     * @return Default double value
     */
    double defaultDouble() default 0d;

    /**
     * Default float value.
     *
     * @return Default float value
     */
    float defaultFloat() default 0f;

    /**
     * Default int value.
     *
     * @return Default int value
     */
    int defaultInt() default 0;

    /**
     * Default long value.
     *
     * @return Default long value
     */
    long defaultLong() default 0L;

    /**
     * Default short value.
     *
     * @return Default short value
     */
    short defaultShort() default (short) 0;

    /**
     * Default string value.
     *
     * @return Default string value
     */
    String defaultString() default "";

    /**
     * Default list value.
     *
     * @return Default list value
     */
    String[] defaultList() default {};

    /**
     * Default map value as a list of string {@code {"key1: value1", "key2: value2", ...}}.
     *
     * @return Default map value
     */
    String[] defaultMap() default {};

    /**
     * Default class value.
     *
     * @return Default class value
     */
    Class<?> defaultClass() default Class.class;

}

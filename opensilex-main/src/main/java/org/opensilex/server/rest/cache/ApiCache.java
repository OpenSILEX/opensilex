//******************************************************************************
//                          ApiProtected.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.server.rest.cache;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Api annotation to enable caching.
 *
 * @author Vincent Migot
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiCache {

    /**
     * Cache category.
     *
     * @return cache category identifier.
     */
    public String category();

    /**
     * Determine if cache is user dependent or not.
     *
     * @return if true cache will be created per user.
     */
    public boolean userDependent() default false;

    /**
     * List of cache categories to clear if this annotation is used on a non-GET API method.
     *
     * @return List of categories identifier to clear.
     */
    public String[] clearCategories() default {};

}

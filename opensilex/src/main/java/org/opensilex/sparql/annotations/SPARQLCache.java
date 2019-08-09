/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql.annotations;

import java.lang.annotation.Documented;
import static java.lang.annotation.ElementType.TYPE;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;
import org.opensilex.sparql.cache.SPARQLCacheManager;
import org.opensilex.sparql.cache.SPARQLNoCacheManager;

/**
 *
 * @author vincent
 */
@Target(TYPE)
@Retention(RUNTIME)
@Documented
public @interface SPARQLCache {
    Class<? extends SPARQLCacheManager> implementation() default SPARQLNoCacheManager.class;
    SPARQLCacheOption[] value() default {};
}

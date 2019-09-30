/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql.annotations;

import java.lang.annotation.Documented;
import static java.lang.annotation.ElementType.FIELD;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;

/**
 *
 * @author Vincent Migot
 */
@Target(FIELD)
@Retention(RUNTIME)
@Documented
public @interface SPARQLProperty {
    
    Class<?> ontology();
    
    String property();
    
    boolean required() default false;
    
    boolean inverse() default false;
}

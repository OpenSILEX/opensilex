/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.module.core.service.sparql.annotations;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 *
 * @author vincent
 */
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(value = SPARQLCache.class)
public @interface SPARQLCacheOption {
    String key();
    String value();
}

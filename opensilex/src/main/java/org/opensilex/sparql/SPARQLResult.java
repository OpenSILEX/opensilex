/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql;

import java.util.function.BiConsumer;

/**
 *
 * @author Vincent Migot
 */
public interface SPARQLResult {

    public String getStringValue(String key);
    
    public void forEach(BiConsumer<? super String, ? super String> action);
}

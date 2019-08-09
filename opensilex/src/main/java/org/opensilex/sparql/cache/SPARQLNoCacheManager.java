/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql.cache;

import java.util.Map;

/**
 *
 * @author vincent
 */
public class SPARQLNoCacheManager implements SPARQLCacheManager{
    public SPARQLNoCacheManager() {
        // Default empty constructor
    }
        
    public SPARQLNoCacheManager(Map<String, String> options) {
        // SPARQLCacheManager must have a constructor with options map
    }
}

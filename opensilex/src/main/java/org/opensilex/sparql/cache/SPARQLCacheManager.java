/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql.cache;

import java.net.URI;

/**
 *
 * @author vincent
 */
public interface SPARQLCacheManager {
    
    public default void putCacheInstance(URI uri, Object obj) {
        
    };

    public default boolean hasCacheInstance(URI uri) {
        return false;
    };

    public default Object getCacheInstance(URI uri) {
        return null;
    };
    
    public default void removeCacheInstance(URI uri) {

    }
}

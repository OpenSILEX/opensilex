//******************************************************************************
//                              RestApplication.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.server.rest.cache;

import java.util.Map;
import org.opensilex.service.Service;
import org.opensilex.service.ServiceDefaultDefinition;

/**
 *
 * @author Vincent Migot
 */
@ServiceDefaultDefinition(
        implementation = NoApiCacheService.class
)
public interface ApiCacheService extends Service {

    /**
     * Static cache category identifier.
     */
    public final static String STATIC_CATEGORY = "static";

    /**
     * Determine if a key exists for the given category in cache.
     *
     * @param category Category to check
     * @param key Key to check
     * @return true if key exists in category or false otherwise.
     */
    public boolean exists(String category, String key);

    /**
     * Get a cached object for the given category and key in cache.
     *
     * @param category cache category
     * @param key cache key
     * @return cached object
     */
    public Object retrieve(String category, String key);

    /**
     * Store an object in cache.
     *
     * @param category object cache category
     * @param key object cache key
     * @param value object value to cache
     */
    public void store(String category, String key, Object value);

    /**
     * Remove an object from cache.
     *
     * @param category object cache category
     * @param key object cache key
     */
    public void remove(String category, String key);

    /**
     * Remove cache for an entire category.
     *
     * @param category category identifier to clear
     */
    public void remove(String category);

    /**
     * Return list of objects matching a given key pattern in cache.
     *
     * @param category cache category
     * @param keyPattern cache key pattern
     * @return Map of object indexed by key
     */
    public Map<Object, Object> getMatching(String category, String keyPattern);

}

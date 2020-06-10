//******************************************************************************
//                              RestApplication.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.server.rest.cache;

import java.util.HashMap;
import java.util.Map;
import org.opensilex.service.BaseService;

/**
 * Cache implementation for no caching.
 *
 * @author Vincent Migot
 */
public class NoApiCacheService extends BaseService implements ApiCacheService {

    public NoApiCacheService() {
        super(null);
    }

    @Override
    public boolean exists(String category, String key) {
        return false;
    }

    @Override
    public Object retrieve(String category, String key) {
        return null;
    }

    @Override
    public void store(String category, String key, Object returnValue) {
        // Do nothing
    }

    @Override
    public void remove(String category, String key) {
        // Do nothing
    }

    @Override
    public Map<Object, Object> getMatching(String category, String keyPattern) {
        return new HashMap<>();
    }

    @Override
    public void remove(String category) {
        // Do nothing
    }

}

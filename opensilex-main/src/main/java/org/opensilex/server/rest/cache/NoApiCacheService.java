/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.server.rest.cache;

import java.util.HashMap;
import java.util.Map;
import org.opensilex.service.BaseService;

/**
 *
 * @author vince
 */
public class NoApiCacheService extends BaseService implements ApiCacheService {

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

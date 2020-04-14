/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.server.rest.cache;

import java.util.Map;
import org.opensilex.service.Service;
import org.opensilex.service.ServiceDefaultDefinition;

/**
 *
 * @author vince
 */
@ServiceDefaultDefinition(
        implementation = JCSApiCacheService.class
)
public interface ApiCacheService extends Service {

    public final static String STATIC_CATEGORY = "static";

    public boolean exists(String category, String key);

    public Object retrieve(String category, String key);

    public void store(String category, String key, Object value);

    public void remove(String category, String key);

    public void remove(String category);

    public Map<Object, Object> getMatching(String category, String keyPattern);

}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.module.core.service.sparql.cache;

import java.net.URI;
import java.time.LocalTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author vincent
 */
public class SPARQLTimeCacheManager implements SPARQLCacheManager {
    private Map<URI, Object> instanceCache = new ConcurrentHashMap<>();
    private Map<URI, LocalTime> instanceLastUsage = new ConcurrentHashMap<>();
    
    private final int cacheDuration;
    private final TimeUnit cacheDurationUnit;
    
    private final static int DEFAULT_INSTANCE_CACHE_DURATION_VALUE = 10;
    private final static TimeUnit DEFAULT_INSTANCE_CACHE_DURATION_UNIT = TimeUnit.MINUTES;
    
    public final static String CACHE_DURATION_VALUE_KEY = "CACHE_DURATION_VALUE_KEY";
    public final static String CACHE_DURATION_UNIT_KEY = "CACHE_DURATION_UNIT_KEY";

    
    public SPARQLTimeCacheManager(Map<String, String> options) {
        if (options.containsKey(CACHE_DURATION_VALUE_KEY)) {
            this.cacheDuration = Integer.valueOf(options.get(CACHE_DURATION_VALUE_KEY));
        } else {
            this.cacheDuration = DEFAULT_INSTANCE_CACHE_DURATION_VALUE;
        }
        
        if (options.containsKey(CACHE_DURATION_UNIT_KEY)) {
            this.cacheDurationUnit = TimeUnit.valueOf(options.get(CACHE_DURATION_UNIT_KEY));
        } else {
            this.cacheDurationUnit = DEFAULT_INSTANCE_CACHE_DURATION_UNIT;
        }
        
        initCacheCleaner();
    }
    
    @Override
    public void putCacheInstance(URI uri, Object obj) {
        instanceCache.put(uri, obj);
        instanceLastUsage.put(uri, LocalTime.now());
    }

    @Override
    public boolean hasCacheInstance(URI uri) {
        return instanceCache.containsKey(uri);
    }

    @Override
    public Object getCacheInstance(URI uri) {
        Object obj = null;
        if (hasCacheInstance(uri)) {
            obj = instanceCache.get(uri);
            instanceLastUsage.put(uri, LocalTime.now());
        }
        return obj;
    }
    
    @Override
    public void removeCacheInstance(URI uri) {
        instanceCache.remove(uri);
        instanceLastUsage.remove(uri);
    }

    private volatile boolean cleaningInstances = false;

    private void initCacheCleaner() {
        ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
        exec.scheduleAtFixedRate(() -> {
            if (!cleaningInstances) {
                cleaningInstances = true;
                LocalTime timeLimit = LocalTime.now().minus(
                        cacheDuration,
                        cacheDurationUnit.toChronoUnit()
                );
                instanceLastUsage.entrySet().forEach((entry) -> {
                    if (entry.getValue().isBefore(timeLimit)) {
                        URI uri = entry.getKey();
                        instanceCache.remove(uri);
                        instanceLastUsage.remove(uri);
                    }
                });
                cleaningInstances = false;
            }
        }, 0, cacheDuration, cacheDurationUnit);
    }

}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 *
 * @author Vincent Migot
 */
public class ServiceManager {

    private Map<Class<? extends Service>, Map<String, Service>> serviceInstanceRegistry = new HashMap<>();

    private Map<String, Service> serviceByNameRegistry = new HashMap<>();
    
    public void register(Class<? extends Service> serviceClass, String name, Service service) {
        if (!serviceInstanceRegistry.containsKey(serviceClass)) {
            serviceInstanceRegistry.put(serviceClass, new HashMap<>());
        }

        serviceInstanceRegistry.get(serviceClass).put(name, service);
        serviceByNameRegistry.put(name, service);
    }
    
    public Map<String, Service> getServices() {
        return Collections.unmodifiableMap(serviceByNameRegistry);
    }

    public void forEachInterface(BiConsumer<Class<? extends Service>, Map<String, Service>> lambda) {
        serviceInstanceRegistry.forEach(lambda);
    }

    public <T extends Service> T getServiceInstance(String name, Class<T> serviceInterface) {
        return (T) serviceByNameRegistry.get(name);
    }
}

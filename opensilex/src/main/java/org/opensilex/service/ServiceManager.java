/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.service;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import org.opensilex.config.ConfigManager;

/**
 *
 * @author vincent
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

    public void forEachInterface(BiConsumer<Class<? extends Service>, Map<String, Service>> lambda) {
        serviceInstanceRegistry.forEach(lambda);
    }

    public <T extends Service> T getServiceInstance(String name, Class<T> serviceInterface) {
        return (T) serviceByNameRegistry.get(name);
    }
    
    public static Service buildServiceInstance(ConfigManager configManager, ServiceConfig serviceConfig) throws
            NoSuchMethodException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Class<? extends Service> serviceClass = serviceConfig.serviceClass();
        
        Class<? extends ServiceConnection> connectionClass = serviceConfig.connectionClass();
        
        if (connectionClass == null) {
            Constructor<? extends Service> constructor = serviceClass.getConstructor();
            return constructor.newInstance();
        } else {
            String configId = serviceConfig.configId();
            Class<?> configClass = serviceConfig.configClass();

            Object config = configManager.loadConfig(configId, configClass);
            ServiceConnection serviceConnection = connectionClass.getConstructor(configClass).newInstance(config);
            for (Constructor<?> constructor : serviceClass.getConstructors()) {
                if (constructor.getParameterCount() == 1) {
                    Parameter parameter = constructor.getParameters()[0];
                    if (parameter.getType().isInstance(serviceConnection)) {
                        return (Service) constructor.newInstance(serviceConnection);
                    }
                }
            }
        }
        
        // TODO manage exception properly
        throw new IllegalArgumentException("No constructor available for service");
    }

}

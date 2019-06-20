/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.service;

import java.lang.reflect.InvocationTargetException;
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

    public void register(Class<? extends Service> serviceClass, String name, Service service) {
        if (!serviceInstanceRegistry.containsKey(serviceClass)) {
            serviceInstanceRegistry.put(serviceClass, new HashMap<>());
        }

        serviceInstanceRegistry.get(serviceClass).put(name, service);
    }

    public void forEachInterface(BiConsumer<Class<? extends Service>, Map<String, Service>> lambda) {
        serviceInstanceRegistry.forEach(lambda);
    }

    public static Service getServiceInstance(ConfigManager configManager, ServiceConfig serviceConfig) throws
            NoSuchMethodException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Class<? extends Service> serviceClass = serviceConfig.serviceClass();
        Class<? extends ServiceConnection> connectionClass = serviceConfig.connectionClass();
        String configId = serviceConfig.configId();
        Class<?> configClass = serviceConfig.configClass();

        Object config = configManager.loadConfig(configId, configClass);
        ServiceConnection serviceConnection = connectionClass.getConstructor(configClass).newInstance(config);
        return serviceClass.getConstructor(connectionClass).newInstance(serviceConnection);
    }

}

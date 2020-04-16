//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Class to manager OpenSilex services.
 *
 * @author Vincent Migot
 */
public final class ServiceManager {

    /**
     * Index map of all named services instances by service classes.
     */
    private Map<Class<? extends Service>, Map<String, Service>> serviceInstanceRegistry = new HashMap<>();

    /**
     * Index map of all services instances by name.
     */
    private Map<String, Service> serviceByNameRegistry = new HashMap<>();

    /**
     * Register a service instance.
     *
     * @param serviceClass service class
     * @param name service name
     * @param service service instance
     */
    public void register(Class<? extends Service> serviceClass, String name, Service service) {
        if (!serviceInstanceRegistry.containsKey(serviceClass)) {
            serviceInstanceRegistry.put(serviceClass, new HashMap<>());
        }

        serviceInstanceRegistry.get(serviceClass).put(name, service);
        serviceByNameRegistry.put(name, service);
    }

    /**
     * Return map of all services instances indexed by name.
     *
     * @return service map
     */
    public Map<String, Service> getServices() {
        return Collections.unmodifiableMap(serviceByNameRegistry);
    }

    /**
     * Iterate over all services classes.
     *
     * @param lambda Function of service class and map of corresponding service instances indexed by name
     */
    public void forEachInterface(BiConsumer<Class<? extends Service>, Map<String, Service>> lambda) {
        serviceInstanceRegistry.forEach(lambda);
    }

    /**
     * Return a service instance by it's name and interface class.
     *
     * @param <T> Service interface
     * @param name Service name
     * @param serviceInterface Service interface
     * @return Service instance
     */
    @SuppressWarnings("unchecked")
    public <T extends Service> T getServiceInstance(String name, Class<T> serviceInterface) {
        T service = (T) serviceByNameRegistry.get(name);

        return service;
    }
}

//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.service;

/**
 * Store service construction parameters.
 *
 * @author Vincent Migot
 */
public final class ServiceConstructorArguments implements ServiceDefinition {

    /**
     * Service implementation.
     */
    private final Class<? extends Service> implementation;

    /**
     * Service config class parameter for constructor if exists.
     */
    private final Class<?> configClass;

    /**
     * Service config identifier parameter for constructor if exists.
     */
    private final String configID;

    /**
     * Service config object used for service construction.
     */
    private final Object config;

    /**
     * Service identifier used for Service constructor parameter.
     */
    private final String serviceID;

    /**
     * Service class used for Service constructor parameter.
     */
    private final Class<? extends Service> serviceClass;

    /**
     * Service instance used for Service constructor parameter.
     */
    private final Service serviceInstance;

    /**
     * Global constructor with all field.
     *
     * @param implementation
     * @param configClass
     * @param configID
     * @param config
     * @param serviceID
     * @param serviceClass
     * @param serviceInstance
     */
    private ServiceConstructorArguments(
            Class<? extends Service> implementation,
            Class<?> configClass,
            String configID,
            Object config,
            String serviceID,
            Class<? extends Service> serviceClass,
            Service serviceInstance
    ) {
        this.implementation = implementation;
        this.configClass = configClass;
        this.configID = configID;
        this.config = config;
        this.serviceID = serviceID;
        this.serviceClass = serviceClass;
        this.serviceInstance = serviceInstance;
    }

    /**
     * Used for services constructed with another service as constructor parameter.
     *
     * @param implementation
     * @param serviceID
     * @param serviceClass
     * @param serviceInstance
     */
    public ServiceConstructorArguments(
            Class<? extends Service> implementation,
            String serviceID,
            Class<? extends Service> serviceClass,
            Service serviceInstance
    ) {
        this(implementation, null, null, null, serviceID, serviceClass, serviceInstance);
    }

    /**
     * Used for services constructed with a configuration as constructor parameter.
     *
     * @param implementation
     * @param configClass
     * @param configID
     * @param config
     */
    public ServiceConstructorArguments(
            Class<? extends Service> implementation,
            Class<?> configClass,
            String configID,
            Object config
    ) {
        this(implementation, configClass, configID, config, null, null, null);
    }

    /**
     * Used for services constructed with no constructor parameter.
     *
     * @param implementation
     */
    public ServiceConstructorArguments(
            Class<? extends Service> implementation
    ) {
        this(implementation, null, null, null, null, null, null);
    }

    @Override
    public Class<? extends Service> implementation() {
        return implementation;
    }

    @Override
    public Class<?> configClass() {
        return configClass;
    }

    @Override
    public String configID() {
        return configID;
    }

    /**
     * Return config constructor parameter instance.
     *
     * @return config constructor parameter instance
     */
    public Object getConfig() {
        return config;
    }

    @Override
    public String serviceID() {
        return serviceID;
    }

    @Override
    public Class<? extends Service> serviceClass() {
        return serviceClass;
    }

    /**
     * Return service constructor parameter instance.
     *
     * @return service constructor parameter instance
     */
    public Service getService() {
        return serviceInstance;
    }
}

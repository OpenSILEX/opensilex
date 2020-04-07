/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.service;

/**
 *
 * @author vince
 */
public class ServiceConstructorArguments implements ServiceDefinition {

    private final Class<? extends Service> implementation;
    private final Class<?> configClass;
    private final String configID;
    private final Object config;
    private final String serviceID;
    private final Class<? extends Service> serviceClass;
    private final Service serviceInstance;

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

    public ServiceConstructorArguments(
            Class<? extends Service> implementation,
            String serviceID,
            Class<? extends Service> serviceClass,
            Service serviceInstance
    ) {
        this(implementation, null, null, null, serviceID, serviceClass, serviceInstance);
    }

    public ServiceConstructorArguments(
            Class<? extends Service> implementation,
            Class<?> configClass,
            String configID,
            Object config
    ) {
        this(implementation, configClass, configID, config, null, null, null);
    }

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

    public Service getService() {
        return serviceInstance;
    }
}

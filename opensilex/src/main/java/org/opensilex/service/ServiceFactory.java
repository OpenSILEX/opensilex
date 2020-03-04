/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.service;

import org.glassfish.hk2.api.Factory;

/**
 *
 * @author vmigot
 */
public abstract class ServiceFactory<T extends Service> implements Factory<T>{

    private Class<? extends T> implementationClass;
    private Class<? extends ServiceConnection> connectionClass;
    private Class<?> connectionConfigClass;
    private Object connectionConfig;
    private Class<?> implementationConfigClass;
    private Object implementationConfig;

    public Class<? extends T> getImplementationClass() {
        return implementationClass;
    }

    public void setImplementationClass(Class<? extends T> implementationClass) {
        this.implementationClass = implementationClass;
    }

    public Class<? extends ServiceConnection> getConnectionClass() {
        return connectionClass;
    }

    public void setConnectionClass(Class<? extends ServiceConnection> connectionClass) {
        this.connectionClass = connectionClass;
    }

    public Class<?> getConnectionConfigClass() {
        return connectionConfigClass;
    }

    public void setConnectionConfigClass(Class<?> connectionConfigClass) {
        this.connectionConfigClass = connectionConfigClass;
    }

    public Object getConnectionConfig() {
        return connectionConfig;
    }

    public void setConnectionConfig(Object connectionConfig) {
        this.connectionConfig = connectionConfig;
    }

    public Class<?> getImplementationConfigClass() {
        return implementationConfigClass;
    }

    public void setImplementationConfigClass(Class<?> implementationConfigClass) {
        this.implementationConfigClass = implementationConfigClass;
    }

    public Object getImplementationConfig() {
        return implementationConfig;
    }

    public void setImplementationConfig(Object implementationConfig) {
        this.implementationConfig = implementationConfig;
    }

    public T getNewInstance() throws Exception {
        if (connectionClass != null) {
            ServiceConnection connection;
            if (connectionConfigClass != null && connectionConfig != null) {
                connection = connectionClass.getConstructor(connectionConfigClass).newInstance(connectionConfig);
            } else {
                connection = connectionClass.getConstructor().newInstance();
            }

            return implementationClass.getConstructor(connectionClass).newInstance(connection);
        } else if (implementationConfigClass != null && implementationConfig != null) {
            return implementationClass.getConstructor(implementationConfigClass).newInstance(implementationConfig);
        } else {
            return implementationClass.getConstructor().newInstance();
        }
    }

    public abstract void startup();
    
    public abstract void shutdown();

    @Override
    public abstract T provide();

    @Override
    public abstract void dispose(T t);
}

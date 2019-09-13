/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.service;

import org.opensilex.utils.ClassInfo;

/**
 *
 * @author vincent
 */
public interface Service {

    //public void startup();
    //public void shutdown();
    
    public default void startup() {

    }

    public default void shutdown() {

    }

    public static boolean hasEmptyConstructor(Class<? extends Service> serviceClass) {
        try {
            return (serviceClass.getConstructor() != null);
        } catch (NoSuchMethodException | SecurityException ex) {
            return false;
        }
    }

    public static boolean isConfigurable(Class<? extends Service> serviceClass) {
        return ClassInfo.getConstructorWithParameterImplementing(serviceClass, ServiceConfig.class) != null;
    }

    public static boolean isConnectable(Class<? extends Service> serviceClass) {
        return ClassInfo.getConstructorWithParameterImplementing(serviceClass, ServiceConnection.class) != null;
    }

    public static ServiceConfig getDefaultConfig(Class<? extends Service> serviceClass) {
        ServiceConfigDefault defaultConfigAnnotation = serviceClass.getAnnotation(ServiceConfigDefault.class);

        return new ServiceConfig() {

            @Override
            public Class<?> configClass() {
                if (defaultConfigAnnotation == null) {
                    return Class.class;
                }
                return defaultConfigAnnotation.configClass();
            }

            @Override
            public String configID() {
                if (defaultConfigAnnotation == null) {
                    return "";
                }
                return defaultConfigAnnotation.configID();
            }

            @Override
            public Class<?> connectionConfig() {
                if (defaultConfigAnnotation == null) {
                    return Class.class;
                }
                return defaultConfigAnnotation.connectionConfig();
            }

            @Override
            public String connectionConfigID() {
                if (defaultConfigAnnotation == null) {
                    return "";
                }
                return defaultConfigAnnotation.connectionConfigID();
            }

            @Override
            public Class<? extends ServiceConnection> connection() {
                if (defaultConfigAnnotation == null) {
                    return ServiceConnection.class;
                }
                return defaultConfigAnnotation.connection();
            }

            @Override
            public Class<? extends Service> implementation() {
                if (defaultConfigAnnotation == null) {
                    return Service.class;
                }
                return defaultConfigAnnotation.implementation();
            }
        };
    }
}

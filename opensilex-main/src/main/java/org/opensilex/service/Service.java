//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.service;

import org.opensilex.OpenSilex;
import org.opensilex.utils.ClassUtils;

/**
 * Interface for service definition.
 *
 * @author Vincent Migot
 */
public interface Service {

    /**
     * Method to initialialize all service setup configuration (before service "startup" but after all modules "setup").
     *
     * @throws Exception in case of setup error
     */
    public default void setup() throws Exception {

    }

    /**
     * Method to start service (when all modules and services are "setup").
     *
     * @throws Exception in case of startup error
     */
    public default void startup() throws Exception {

    }

    /**
     * Method to stop service (after all modules "shutdown" but before all modules and services "clean").
     *
     * @throws Exception in case of shutdown error
     */
    public default void shutdown() throws Exception {

    }

    /**
     * Method to clean service state (after all services "shutdown").
     *
     * @throws Exception
     */
    public default void clean() throws Exception {

    }

    /**
     * Define OpenSilex instance (see org.opensilex.service.BaseService for default implementation).
     *
     * @param opensilex current application instance
     */
    public void setOpenSilex(OpenSilex opensilex);

    /**
     * Return current OpenSilex instance (see org.opensilex.service.BaseService for default implementation).
     *
     * @return current application instance
     */
    public OpenSilex getOpenSilex();

    /**
     * Define service constructor arguments (see org.opensilex.service.BaseService for default implementation).
     *
     * @param args constructor arguments
     */
    public void setServiceConstructorArguments(ServiceConstructorArguments args);

    /**
     * Return service constructor arguments (see org.opensilex.service.BaseService for default implementation).
     *
     * @return service constructor arguments
     */
    public ServiceConstructorArguments getServiceConstructorArguments();

    /**
     * Helper method to determine if a service class has an empty constructor (no arguments).
     *
     * @param serviceClass Service class to check
     * @return true if empty constructor exists and false otherwise
     */
    public static boolean hasEmptyConstructor(Class<? extends Service> serviceClass) {
        try {
            return (serviceClass.getConstructor() != null);
        } catch (NoSuchMethodException | SecurityException ex) {
            return false;
        }
    }

    /**
     * Helper method to determine if a service class has a constructor accepting a ServiceConfig instance as parameter.
     *
     * @param serviceClass Service class to check
     * @return true if constructor with ServiceConfig parameter exists and false otherwise
     */
    public static boolean isConfigurable(Class<? extends Service> serviceClass) {
        return ClassUtils.getConstructorWithParameterImplementing(serviceClass, ServiceConfig.class) != null;
    }

    /**
     * Helper method to determine if a service class has a constructor accepting a Service instance as parameter.
     *
     * @param serviceClass Service class to check
     * @return true if constructor with Service parameter exists and false otherwise
     */
    public static boolean isConnectable(Class<? extends Service> serviceClass) {
        return ClassUtils.getConstructorWithParameterImplementing(serviceClass, Service.class) != null;
    }

    /**
     * Helper method to return default service definition looking for @ServiceDefaultDefinition annotation on given class.
     *
     * @param serviceClass Class to analyse
     * @return Defulat service definition
     */
    public static ServiceDefinition getDefaultServiceDefinition(Class<? extends Service> serviceClass) {
        ServiceDefaultDefinition defaultConfigAnnotation = serviceClass.getAnnotation(ServiceDefaultDefinition.class);

        return new ServiceDefinition() {

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
            public Class<? extends Service> implementation() {
                if (defaultConfigAnnotation == null) {
                    return Service.class;
                }
                return defaultConfigAnnotation.implementation();
            }

            @Override
            public String serviceID() {
                if (defaultConfigAnnotation == null) {
                    return "";
                }
                return defaultConfigAnnotation.serviceID();
            }

            @Override
            public Class<? extends Service> serviceClass() {
                if (defaultConfigAnnotation == null) {
                    return Service.class;
                }
                return defaultConfigAnnotation.serviceClass();
            }
        };
    }
}

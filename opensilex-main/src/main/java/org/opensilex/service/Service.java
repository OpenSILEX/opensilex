//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.service;

import org.opensilex.OpenSilex;
import org.opensilex.utils.ClassUtils;

/**
 *
 * @author Vincent Migot
 */
public interface Service {

    public default void setup() throws Exception {

    }

    public default void startup() throws Exception {

    }

    public default void shutdown() throws Exception {

    }

    public default void clean() throws Exception {

    }

    public default void clean() throws Exception {

    }

    public void setOpenSilex(OpenSilex opensilex);

    public OpenSilex getOpenSilex();

    public void setServiceConstructorArguments(ServiceConstructorArguments args);

    public ServiceConstructorArguments getServiceConstructorArguments();

    public static boolean hasEmptyConstructor(Class<? extends Service> serviceClass) {
        try {
            return (serviceClass.getConstructor() != null);
        } catch (NoSuchMethodException | SecurityException ex) {
            return false;
        }
    }

    public static boolean isConfigurable(Class<? extends Service> serviceClass) {
        return ClassUtils.getConstructorWithParameterImplementing(serviceClass, ServiceConfig.class) != null;
    }

    public static boolean isConnectable(Class<? extends Service> serviceClass) {
        return ClassUtils.getConstructorWithParameterImplementing(serviceClass, Service.class) != null;
    }

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

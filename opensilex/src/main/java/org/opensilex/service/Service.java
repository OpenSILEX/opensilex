//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.service;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.opensilex.OpenSilex;
import org.opensilex.module.ModuleConfig;
import org.opensilex.module.ModuleNotFoundException;
import org.opensilex.OpenSilexModule;
import org.opensilex.utils.ClassUtils;

/**
 *
 * @author Vincent Migot
 */
public interface Service {

    public default void startup() throws Exception {

    }

    public default void shutdown() throws Exception {

    }

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
        return ClassUtils.getConstructorWithParameterImplementing(serviceClass, ServiceConnection.class) != null;
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

    public default void setModule(OpenSilexModule module) {
        Service.registerModule(this.getClass(), module);
    }

    public default <T extends OpenSilexModule> T getModule(Class<T> moduleClass) {
        return Service.getModule(this.getClass(), moduleClass);
    }

    static Map<Class<? extends Service>, OpenSilexModule> moduleByService = new HashMap<>();

    public static void registerModule(Class<? extends Service> serviceClass, OpenSilexModule module) {
        moduleByService.put(serviceClass, module);
    }

    public static <T extends OpenSilexModule> T getModule(Class<? extends Service> serviceClass, Class<T> moduleClass) {
        return (T) moduleByService.get(serviceClass);
    }
}

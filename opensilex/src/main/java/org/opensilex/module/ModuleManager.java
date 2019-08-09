/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.module;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;
import java.util.function.Consumer;

import org.apache.commons.io.FileUtils;
import org.opensilex.OpenSilex;
import org.opensilex.config.ConfigManager;
import org.opensilex.module.dependency.DependencyManager;
import org.opensilex.service.Service;
import org.opensilex.service.ServiceConfig;
import org.opensilex.service.ServiceManager;
import org.opensilex.utils.ClassInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author vincent
 */
public class ModuleManager {

    private final static Logger LOGGER = LoggerFactory.getLogger(ModuleManager.class);

    private final static String DEPENDENCIES_LIST_CACHE_FILE = ".opensilex.dependencies";

    public static List<URL> readDependenciesList(Path baseDirectory) {
        try {
            File dependencyFile = baseDirectory.resolve(DEPENDENCIES_LIST_CACHE_FILE).toFile();
            if (dependencyFile.isFile()) {
                List<URL> dependencyURLs = new ArrayList<>();
                for (String dependency : FileUtils.readLines(dependencyFile, StandardCharsets.UTF_8.name())) {
                    dependencyURLs.add(new URL(dependency));
                };

                return dependencyURLs;
            } else {
                return null;
            }
        } catch (IOException ex) {
            LOGGER.error("Error while reading dependency file", ex);
            return null;
        }
    }

    public static void writeDependenciesList(Path baseDirectory, List<URL> dependencies) {
        try {
            File dependencyFile = baseDirectory.resolve(DEPENDENCIES_LIST_CACHE_FILE).toFile();
            FileUtils.writeLines(dependencyFile, dependencies);
        } catch (IOException ex) {
            LOGGER.error("Error while writing dependency file", ex);
        }
    }

    private List<Module> modules;

    private ConfigManager configManager;
    private ServiceManager services;

    public List<URL> loadModulesWithDependencies(List<URL> modulesJarURLs) {
        try {
            DependencyManager dependencyManager = new DependencyManager(
                    ClassInfo.getPomFile(OpenSilex.class, "org.opensilex", "opensilex")
            );

            List<URL> dependenciesURL = dependencyManager.loadModulesDependencies(modulesJarURLs);
            dependenciesURL.addAll(modulesJarURLs);

            registerDependencies(dependenciesURL);
            return dependenciesURL;
        } catch (Exception ex) {
            LOGGER.error("Error while loading modules with dependencies", ex);
        }

        return null;
    }

    public void registerDependencies(List<URL> dependenciesURL) {
        if (LOGGER.isDebugEnabled()) {
            dependenciesURL.forEach((dependencyURL) -> {
                LOGGER.debug("Loaded dependency: " + dependencyURL.toString());
            });
        }

        if (dependenciesURL.size() > 0) {
            URLClassLoader classLoader = new URLClassLoader(
                    dependenciesURL.toArray(new URL[dependenciesURL.size()]),
                    Thread.currentThread().getContextClassLoader()
            );
            LOGGER.debug("Module registred, jar URLs added to classpath");
            Thread.currentThread().setContextClassLoader(classLoader);
        } else {
            LOGGER.debug("No external module found !");
        }
    }

    public void forEachModule(Consumer<Module> lambda) {
        getModules().forEach(lambda);
    }

    public void setApplication(OpenSilex app) {
        // Initialize service loader to find all modules and define this application to them
        LOGGER.debug("Loading modules classes");
        forEachModule((Module module) -> {
            module.setApplication(app);
            LOGGER.debug("Module class found: " + module.getClass().getCanonicalName());
        });
    }

    public Iterable<Module> getModules() {
        if (modules == null) {
            modules = new ArrayList<>();
            ServiceLoader.load(Module.class, Thread.currentThread().getContextClassLoader())
                    .forEach(modules::add);
        }

        return Collections.unmodifiableList(modules);

    }

    public static List<URL> listModulesURLs(Path baseDirectory) {
        File modulesDirectory = baseDirectory.resolve("modules").toFile();
        File[] modulesList = modulesDirectory.listFiles();

        LOGGER.debug("Start listing jar module files in directory: " + modulesDirectory.getPath());

        List<URL> modulesJarURLs = new ArrayList<>();
        if (modulesList != null) {
            for (File moduleFile : modulesList) {
                modulesJarURLs.add(getModuleURLFromFile(moduleFile));
            }
        } else {
            LOGGER.debug("Modules directory doesn't exists !");
        }

        return modulesJarURLs;
    }

    private static URL getModuleURLFromFile(File moduleFile) {
        URL result = null;

        if (moduleFile.isFile() && moduleFile.toString().endsWith(".jar")) {
            try {
                URL jarUrl = moduleFile.toURI().toURL();
                result = jarUrl;
                LOGGER.debug("Registering jar module file: " + moduleFile.getPath());
            } catch (MalformedURLException ex) {
                LOGGER.error("Error while registering module: " + moduleFile.getPath(), ex);
            }
        } else {
            LOGGER.warn("Ignoring module : " + moduleFile.getPath());
        }

        return result;
    }

    public void loadServices(ServiceManager serviceManager) {
        this.services = serviceManager;
        for (Module module : getModules()) {

            // TODO Load services properly
            ModuleConfig moduleConfig = module.getConfig();
            if (moduleConfig != null && moduleConfig.services() != null) {
                try {
                    for (String serviceName : moduleConfig.services().keySet()) {

                        ServiceConfig serviceConfig = moduleConfig.services().get(serviceName);
                        Service service = ServiceManager.getServiceInstance(configManager, serviceConfig);
                        services.register(serviceConfig.serviceClass(), serviceName, service);
                    }
                } catch (NoSuchMethodException
                        | SecurityException
                        | InstantiationException
                        | IllegalAccessException
                        | IllegalArgumentException
                        | InvocationTargetException ex) {

                    // TODO manage error
                    LOGGER.error("WTF ? -->", ex);
                }
            }
        }
    }

    public void init() {
        for (Module module : getModules()) {
            module.init();
        }
    }

    public void clean() {
        for (Module module : getModules()) {
            module.clean();
        }
    }

    public void loadConfigs(ConfigManager configManager) {
        this.configManager = configManager;
        for (org.opensilex.module.Module module : getModules()) {
            String configId = module.getConfigId();
            Class<? extends ModuleConfig> configClass = module.getConfigClass();

            if (configId != null && configClass != null) {
                ModuleConfig config = configManager.loadConfig(configId, configClass);
                module.setConfig(config);
            }
        }

    }
}

//******************************************************************************
//                          OpenSilexModuleManager.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.opensilex.config.ConfigManager;
import org.opensilex.dependencies.DependencyManager;
import org.opensilex.service.Service;
import org.opensilex.service.ServiceManager;
import org.opensilex.utils.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Module manager for OpenSilex applications.
 *
 * Load modules with dependencies and store references
 *
 * @author Vincent Migot
 */
public class OpenSilexModuleManager {

    /**
     * List of build-in modules ordered by their loading order.
     */
    private final static List<String> BUILD_IN_MODULES_ORDER = new ArrayList<String>() {
        {
            add("opensilex-main");
            add("opensilex-fs");
            add("opensilex-nosql");
            add("opensilex-sparql");
            add("opensilex-security");
            add("opensilex-core");
            add("opensilex-front");
        }
    };

    /**
     * Map of ignored module, key is module class and value is JAR artifact name.
     */
    private final static Map<String, String> IGNORED_MODULES = new HashMap<>();

    /**
     * Class Logger.
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(OpenSilexModuleManager.class);

    /**
     * Dependencies cache file to avoid unneeded multiple downloads.
     */
    private final static String DEPENDENCIES_LIST_CACHE_FILE = ".opensilex.dependencies";

    /**
     * Subfolder where JAR modules are located.
     */
    private final static String MODULES_JAR_FOLDER = "modules";

    /**
     * Dependency manager reference.
     */
    private final DependencyManager dependencyManager;

    /**
     * System base directory.
     */
    private final Path baseDirectory;

    /**
     * System configuration.
     */
    private final OpenSilexConfig systemConfig;

    /**
     * Loaded modules URLs list.
     */
    private final Set<URL> modulesURLs;

    /**
     * Constructor for module manager.
     *
     * @param dependencyManager related dependency manager
     * @param baseDirectory system base directory
     * @param systemConfig system configuration
     */
    public OpenSilexModuleManager(DependencyManager dependencyManager, Path baseDirectory, OpenSilexConfig systemConfig) {
        this.dependencyManager = dependencyManager;
        this.baseDirectory = baseDirectory;
        this.systemConfig = systemConfig;
        this.modulesURLs = loadModulesWithDependencies();
    }

    /**
     * Return list of loaded modules JAR URLs.
     *
     * @return list of loaded modules JAR URLs
     */
    public Set<URL> getModulesURLs() {
        return modulesURLs;
    }

    /**
     * Load modules with their dependencies, downloading them if needed.
     *
     * @return list of loaded JAR URLs
     */
    private Set<URL> loadModulesWithDependencies() {
        // Read existing dependencies from cache file
        Set<URL> readDependencies = OpenSilexModuleManager.readDependencies(baseDirectory);
//        Set<URL> readDependencies = new HashSet<>();

        // Get list of modules URL
        Set<URL> urls = OpenSilexModuleManager.listModulesURLs(baseDirectory);

        // If some modules are not listed in dependencies or no dependencies were read
        if (!readDependencies.containsAll(urls)) {
            // If some modules have not been previously registred
            List<URL> missingModules = new ArrayList<URL>(urls);
            missingModules.removeAll(readDependencies);

            // Get modules dependencies and load them
            Set<URL> dependencies = loadModulesWithDependencies(dependencyManager, urls);

            // Rewrite old & new dependencies in cache file
            dependencies.addAll(readDependencies);
            dependencies.removeAll(urls);
            OpenSilexModuleManager.writeDependencies(baseDirectory, dependencies);

            registerDependencies(readDependencies);
        } else {
            // Otherwise simply register known dependencies
            registerDependencies(readDependencies);
        }

        registerDependencies(urls);

        addOptionalModulesOrder(systemConfig.modulesOrder());

        setIgnoredModules(systemConfig.ignoredModules());

        return urls;
    }

    /**
     * Utility method to read dependencies from cache file.
     *
     * @param baseDirectory Directory where dependency cache file is.
     *
     * @return Lsit of JAR dependencies URL
     */
    private static Set<URL> readDependencies(Path baseDirectory) {
        try {
            // Check if depndency file exist
            File dependencyFile = baseDirectory.resolve(DEPENDENCIES_LIST_CACHE_FILE).toFile();
            Set<URL> dependencyURLs = new HashSet<>();

            if (dependencyFile.isFile()) {
                // If it's a file read all lines and add in the dependencyURLs list
                for (String dependency : FileUtils.readLines(dependencyFile, StandardCharsets.UTF_8.name())) {
                    dependencyURLs.add(new URL(dependency));
                }
            }

            // Return the list
            return dependencyURLs;
        } catch (IOException ex) {
            LOGGER.error("Error while reading dependency file", ex);
            return null;
        }
    }

    /**
     * Utility method to write dependencies URL to a cache file.
     *
     * @param baseDirectory Directory where dependency cache file is.
     * @param dependencies Depndencies JAR URL list.
     */
    private static void writeDependencies(Path baseDirectory, Set<URL> dependencies) {
        try {
            File dependencyFile = baseDirectory.resolve(DEPENDENCIES_LIST_CACHE_FILE).toFile();
            if (dependencies.size() > 0) {
                FileUtils.writeLines(dependencyFile, dependencies);
            }
        } catch (IOException ex) {
            LOGGER.error("Error while writing dependency file", ex);
        }
    }

    /**
     * List of loaded modules.
     */
    private List<OpenSilexModule> modules;

    /**
     * Service manager reference.
     */
    private ServiceManager services;

    /**
     * Load modules and their dependencies.
     *
     * @param dependencyManager Dependency manager to load dependencies
     * @param modulesJarURLs List of module JAR URLs
     * @return List of all dependencies for modules and the modules themselves
     */
    private Set<URL> loadModulesWithDependencies(DependencyManager dependencyManager, Set<URL> modulesJarURLs) {
        try {
            // Load module dependencies and get the list
            Set<URL> dependenciesURL = dependencyManager.loadModulesDependencies(modulesJarURLs);

            // Register all dependencies and modules
            registerDependencies(dependenciesURL);
            return dependenciesURL;
        } catch (Exception ex) {
            LOGGER.error("Error while loading modules with dependencies", ex);
        }

        return null;
    }

    /**
     * Register all depents JAR URL in list for use with class loaders.
     *
     * @param dependenciesURL List of dependencies to register
     */
    private void registerDependencies(Set<URL> dependenciesURL) {
        if (LOGGER.isDebugEnabled()) {
            dependenciesURL.forEach((dependencyURL) -> {
                LOGGER.debug("Added dependency to classpath: " + dependencyURL.getPath());
            });
        }

        // Load dependencies through URL Class Loader based on actual class loader
        if (dependenciesURL.size() > 0) {
            URLClassLoader classLoader = new URLClassLoader(
                    dependenciesURL.toArray(new URL[dependenciesURL.size()]),
                    Thread.currentThread().getContextClassLoader()
            );
            LOGGER.debug("JAR URLs added to classpath");

            // Set the newly created class loader as the main one
            Thread.currentThread().setContextClassLoader(classLoader);
        } else {
            LOGGER.debug("No URLs added to classpath !");
        }
    }

    /**
     * Utility method to iterate through modules.
     *
     * <pre>
     * Example:
     * <code>
     * moduleManager.forEachModule((OpenSilexModule module) -&gt; {
     *      // DO STUFF WITH MODULE..
     * });
     * </code>
     * </pre>
     *
     * @param lambda Lambda to realize action on modules.
     */
    public void forEachModule(Consumer<OpenSilexModule> lambda) {
        getModules().forEach(lambda);
    }

    /**
     * Return an Iterable of modules to do custom loop logic.
     *
     * @return Iterable of modules
     */
    public Iterable<OpenSilexModule> getModules() {
        if (modules == null) {

            modules = new ArrayList<>();
            Iterator<OpenSilexModule> i = ServiceLoader.load(OpenSilexModule.class, OpenSilex.getClassLoader()).iterator();
            while (i.hasNext()) {
                modules.add(i.next());
            }
            modules = modules
                    .stream()
                    .filter((m) -> {
                        return !IGNORED_MODULES.containsKey(m.getClass().getCanonicalName());
                    })
                    .sorted((m1, m2) -> {
                        String artifact1 = ClassUtils.getProjectIdFromClass(m1.getClass());
                        String artifact2 = ClassUtils.getProjectIdFromClass(m2.getClass());

                        int index1 = BUILD_IN_MODULES_ORDER.indexOf(artifact1);
                        int index2 = BUILD_IN_MODULES_ORDER.indexOf(artifact2);

                        if (index1 == index2) {
                            return 0;
                        }

                        if (index1 == -1) {
                            return 1;
                        }

                        if (index2 == -1) {
                            return -1;
                        }

                        return index1 - index2;
                    })
                    .map(m -> m)
                    .collect(Collectors.toList());
        }

        return modules;
    }

    /**
     * Add a module specific ordering (but after built-in).
     *
     * @param modulesOrder list of modules order identifier
     */
    public void addOptionalModulesOrder(List<String> modulesOrder) {
        if (modulesOrder != null && !modulesOrder.isEmpty()) {
            for (String optionalModule : modulesOrder) {
                if (!BUILD_IN_MODULES_ORDER.contains(optionalModule)) {
                    BUILD_IN_MODULES_ORDER.add(optionalModule);
                }
            }

            modules = null;
        }
    }

    /**
     * Defined ignored module map.
     *
     * Key is module class and value is module jar file name.
     *
     * @param ignoredModules Map of ignored modules.
     */
    public void setIgnoredModules(Map<String, String> ignoredModules) {
        if (ignoredModules != null) {
            IGNORED_MODULES.putAll(ignoredModules);

            modules = null;
        }
    }

    /**
     * Utility method to get modules URL inside MODULES_JAR_FOLDER subdirectory of the given directory parameter.
     *
     * @param baseDirectory Directory to look in
     * @return List of modules JAR URL found
     */
    private static Set<URL> listModulesURLs(Path baseDirectory) {
        // Find the subdirectory
        File modulesDirectory = baseDirectory.resolve(MODULES_JAR_FOLDER).toFile();

        // Get all files within
        File[] modulesList = modulesDirectory.listFiles();

        LOGGER.debug("Start listing jar module files in directory: " + modulesDirectory.getPath());

        // Filter all JAR found
        Set<URL> modulesJarURLs = new HashSet<>();
        if (modulesList != null) {
            for (File moduleFile : modulesList) {
                LOGGER.debug("Module found: " + moduleFile.getName());
                URL jarURL = getModuleURLFromFile(moduleFile);
                if (jarURL != null) {
                    modulesJarURLs.add(jarURL);
                }
            }
        } else {
            LOGGER.debug("Modules directory doesn't exists !");
        }

        // Return the list
        return modulesJarURLs;
    }

    /**
     * Return corresonding JAR URL of the given file or null.
     *
     * @param moduleFile The file to check
     * @return JAR URL or null
     */
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

    /**
     * Register all module's configured services in the given services manager.
     *
     * @param serviceManager Service manager to use
     * @throws Exception rethrow exceptions on service registring
     */
    @SuppressWarnings("unchecked")
    public void registerServices(ServiceManager serviceManager) throws Exception {
        this.services = serviceManager;
        // Iterate over modules
        for (OpenSilexModule module : getModules()) {

            Object moduleConfig = module.getConfig();
            // If module has configuration
            if (moduleConfig != null) {
                // Iterate of module configuration interface methods
                for (Method m : moduleConfig.getClass().getMethods()) {
                    try {
                        // If configuration parameter match org.opensilex.service.Service class
                        if (Service.class.isAssignableFrom(m.getReturnType())) {
                            // Get service instance from configuration
                            Service service = (Service) m.invoke(moduleConfig);
                            Class<? extends Service> serviceInterface = (Class<? extends Service>) m.getReturnType();
                            // Register service instance
                            services.register(serviceInterface, m.getName(), service);
                        }
                    } catch (Exception ex) {
                        LOGGER.error("Fail to load service: " + m.getName() + " in module config: " + module.getConfigId(), ex);
                        throw ex;
                    }

                }
            }
        }
    }

    /**
     * Return list of modules implementing the given interface.
     *
     * @param <T> Interface class parameter
     * @param extensionInterface Interface class
     * @return List of found modules as T interface
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> getModulesImplementingInterface(Class<T> extensionInterface) {
        List<T> modules = new ArrayList<>();
        forEachModule((OpenSilexModule m) -> {
            if (extensionInterface.isAssignableFrom(m.getClass())) {
                modules.add((T) m);
            }
        });

        return modules;
    }

    /**
     * Load all modules configurations.
     *
     * @param configManager Configuration manager instance
     */
    public void loadConfigs(ConfigManager configManager) {
        // Iterate over modules
        for (OpenSilexModule module : getModules()) {
            // Get module configuration identifier and class
            String configId = module.getConfigId();
            Class<?> configClass = module.getConfigClass();

            // If module is configurable
            if (configId != null && configClass != null) {
                // Load configuration with manager
                Object config = configManager.loadConfig(configId, configClass);
                // Affect loaded configuration to module
                module.setConfig(config);
            }
        }

    }

    /**
     * Return the module instance by is class.
     *
     * @param <T> Module class parameter
     * @param moduleClass Module class
     * @return Module instance
     * @throws OpenSilexModuleNotFoundException Throw exception if module is not found
     */
    @SuppressWarnings("unchecked")
    public <T extends OpenSilexModule> T getModuleByClass(Class<T> moduleClass) throws OpenSilexModuleNotFoundException {
        for (OpenSilexModule module : getModules()) {
            if (module.getClass().equals(moduleClass)) {
                return (T) module;
            }
        }

        throw new OpenSilexModuleNotFoundException(moduleClass);
    }
}

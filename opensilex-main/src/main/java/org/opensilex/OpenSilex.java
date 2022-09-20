//******************************************************************************
//                            OpenSilex.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.opensilex.config.ConfigManager;
import org.opensilex.dependencies.DependencyManager;
import org.opensilex.server.ServerModule;
import org.opensilex.service.Service;
import org.opensilex.service.ServiceManager;
import org.opensilex.utils.ClassUtils;
import org.opensilex.utils.LogFilter;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <pre>
 * This is the main class for OpenSilex Application.
 *
 * It's a configurable and extensible module system.
 * This class use the singleton pattern.
 * After initialization with a directory, a main config file and a profile identifier,
 * it loads all jar files in "modules" subfolder, and then it initialize all existing modules
 * in application classpath with their own configuration and services.
 *
 * Notes:
 * Module management code is delegate to org.opensilex.OpenSilexModuleManager
 * Class Configuration management code is delegate to org.opensilex.config.ConfigManager
 * Class Service management code is delegate to org.opensilex.service.ServiceManager
 * </pre>
 *
 * @author Vincent Migot
 */
public final class OpenSilex {

    /**
     * Class Logger.
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(OpenSilex.class);

    /**
     * Default application language.
     */
    public final static String DEFAULT_LANGUAGE = "en";

    /**
     * Production profile identifier.
     */
    public final static String PROD_PROFILE_ID = "prod";

    /**
     * Test profile identifier.
     */
    public final static String TEST_PROFILE_ID = "test";

    /**
     * Development profile identifier.
     */
    public final static String DEV_PROFILE_ID = "dev";

    /**
     * Special profile used when we need to run an OpenSILEX instance (Swagger/maven build, custom OpenSILEX command)
     * without running the whole OpenSIELX server
     */
    public final static String INTERNAL_OPERATIONS_PROFILE_ID = "internal_operations";


    /**
     * Environment key for OpenSilex base directory.
     */
    public final static String BASE_DIR_ENV_KEY = "OPENSILEX_BASE_DIRECTORY";

    /**
     * Environment key for OpenSilex profile identifier.
     */
    public final static String PROFILE_ID_ENV_KEY = "OPENSILEX_PROFILE_ID";

    /**
     * Environment key for OpenSilex main configuration file.
     */
    public final static String CONFIG_FILE_ENV_KEY = "OPENSILEX_CONFIG_FILE";

    /**
     * Command line argument key for OpenSilex base directory.
     */
    public final static String BASE_DIR_ARG_KEY = "BASE_DIRECTORY";

    /**
     * Command line argument key for OpenSilex profile identifier.
     */
    public final static String PROFILE_ID_ARG_KEY = "PROFILE_ID";

    /**
     * Command line argument key for OpenSilex configuration file.
     */
    public final static String CONFIG_FILE_ARG_KEY = "CONFIG_FILE";

    /**
     * Command line argument key for OpenSilex debug flag.
     */
    public final static String DEBUG_ARG_KEY = "DEBUG";

    /**
     * Command line argument key for OpenSilex no-cache flag.
     */
    public final static String NO_CACHE_ARG_KEY = "NO-CACHE";

    /**
     * Store reference to shutdown hook to avoid duplication on reset.
     */
    private Thread shutdownHook;

    /**
     * <pre>
     * Main method to setup Opensilex instance based on command line arguments.
     *
     * It uses the following algorithm:
     * - Define Base directory: Set by default with environment variable "OPENSILEX_BASE_DIRECTORY"
     * - If "args" array contains "BASE_DIRECTORY" parameter override base directory value
     * - If none of these values are defined use java system property "user.dir" corresponding to current user working directory
     *   see: https://docs.oracle.com/javase/tutorial/essential/environment/sysprop.html
     * - Define Profile identifier Set by default with environment variable "OPENSILEX_PROFILE_ID"
     * - If "args" array contains "PROFILE_ID" parameter override profile identifier value
     * - If none of these values are defined use "prod" as default
     * - Define Configuration file Set by default with environment variable "OPENSILEX_CONFIG_FILE"
     * - If "args" array contains "CONFIG_FILE" parameter override config file value
     * - Otherwise use only application default configuration including in sources depending of profile
     * </pre>
     *
     * @param args Command line arguments array
     * @param forceDebug Option to force debug mode
     * @return OpenSilex setup arguments
     */
    public static OpenSilexSetup createSetup(String[] args, boolean forceDebug) {
        List<Object> cliArgsList = new ArrayList<>(args.length);

        // Initialize with existing environment variables
        String baseDirectory = System.getenv(BASE_DIR_ENV_KEY);
        String configFile = System.getenv(CONFIG_FILE_ENV_KEY);
        String profileId = System.getenv(PROFILE_ID_ENV_KEY);

        boolean debug = false;

        boolean noCache = false;

        // Override with command line arguments values
        for (String arg : args) {
            if (arg.startsWith("--" + BASE_DIR_ARG_KEY + "=")) {
                // For base directory
                baseDirectory = arg.split("=", 2)[1];
            } else if (arg.startsWith("--" + PROFILE_ID_ARG_KEY + "=")) {
                // For profile identifier
                profileId = arg.split("=", 2)[1];
            } else if (arg.startsWith("--" + CONFIG_FILE_ARG_KEY + "=")) {
                // For configuration file
                configFile = arg.split("=", 2)[1];
            } else if (arg.startsWith("--" + DEBUG_ARG_KEY) && !arg.equalsIgnoreCase("--" + DEBUG_ARG_KEY + "=false")) {
                // For configuration file
                debug = true;

            } else if (arg.startsWith("--" + NO_CACHE_ARG_KEY) && !arg.equalsIgnoreCase("--" + NO_CACHE_ARG_KEY + "=false")) {
                // For configuration file
                noCache = true;

            } else {
                // Otherwise add argument to the remaining list
                cliArgsList.add(arg);
            }
        }

        debug = debug || forceDebug;

        // Set default value for base directory if not set previously
        if (baseDirectory == null || baseDirectory.equals("")) {
            baseDirectory = getDefaultBaseDirectory().toString();
        }

        // Set default profile identifier if not set previously
        if (profileId == null) {
            profileId = PROD_PROFILE_ID;
        }

        File cfgFile = null;
        if (configFile != null && !configFile.isEmpty()) {
            try {
                cfgFile = Paths.get(configFile).toFile();
                if (!cfgFile.exists() || !cfgFile.isFile()) {
                    LOGGER.warn("Invalid config file (ignored): " + cfgFile.getAbsolutePath());
                    cfgFile = null;
                }
            } catch (Exception ex) {
                LOGGER.warn("Error while loading config file (ignored): " + cfgFile.getAbsolutePath(), ex);
                cfgFile = null;
            }
        }

        return new OpenSilexSetup(
                Paths.get(baseDirectory),
                profileId,
                cfgFile,
                debug,
                noCache,
                args,
                cliArgsList
        );
    }

    /**
     * Create OpenSilex setup arguments.
     *
     * @param args Command line arguments array
     * @return OpenSilex setup arguments
     */
    public static OpenSilexSetup createSetup(String[] args) {
        return createSetup(args, false);
    }

    /**
     * <pre>
     * Helper method to call easily setup method programatically.
     * <code>
     * OpenSilex.setup(new HashMap&lt;String, String&gt;() {
     *     {
     *         put(OpenSilex.PROFILE_ID_ARG_KEY, OpenSilex.DEV_PROFILE_ID);
     *         put(OpenSilex.CONFIG_FILE_ARG_KEY, configFile);
     *         put(OpenSilex.DEBUG_ARG_KEY, "true");
     *     }
     * });
     * </code>
     * </pre>
     *
     * @param args Map of arguments
     * @return The command line arguments array without the Opensilex parameters
     * @throws Exception
     */
    public static OpenSilex createInstance(Map<String, String> args) throws Exception {
        return createInstance(createSetup(getArgs(args)), true);
    }

    /**
     * Transform map to an array of arguments "--key=value".
     *
     * @param args map to transform
     * @return list of transfromed args
     */
    private static String[] getArgs(Map<String, String> args) {
        List<String> argsList = new ArrayList<>(args.size());
        args.forEach((String key, String value) -> {
            argsList.add("--" + key + "=" + value);
        });

        return argsList.toArray(new String[0]);
    }

    /**
     * Create an OpenSilex instance and start it.
     *
     * @param setup Instance configuration
     * @param autoStart Flag to automatically start instance
     * @return Started OpenSilex instance
     * @throws Exception in case of initialization or startup exception
     */
    public static OpenSilex createInstance(OpenSilexSetup setup, boolean autoStart) throws Exception {
        try {
            OpenSilex instance = buildInstance(setup);

            instance.initialize();

            if (autoStart) {
                instance.startup();
            }

            return instance;

        } catch (Exception ex) {
            LOGGER.error("Fail to create and initialize OpenSILEX instance", ex);
            throw ex;
        }
    }

    /**
     * Initialize intance.
     *
     * @throws Exception
     */
    private void initialize() throws Exception {
        LOGGER.debug("Initialize instance");
        LOGGER.debug("Build global configuration");
        configManager.build(setup.getBaseDirectory(), moduleManager.getModules(), setup.getProfileId(), setup.getConfigFile(), systemConfig);

        LOGGER.debug("Load modules configuration");
        moduleManager.loadConfigs(configManager);

        LOGGER.debug("Resgister modules services");
        moduleManager.registerServices(serviceManager);
        LOGGER.debug("Instance initialized");
    }

    /**
     * Build OpenSilex application from setup.
     *
     * @param setup OpenSilex configuration
     * @return OpenSilex instance
     * @throws Exception in case of initialization problem
     */
    private static OpenSilex buildInstance(OpenSilexSetup setup) throws Exception {

        // Try to find logback.xml file in OpenSilex base directory to initialize logger configuration
        File logConfigFile = setup.getBaseDirectory().resolve("logback.xml").toFile();
        loadLoggerConfig(logConfigFile, true);

        // Try to find logback-{profile-id}.xml file in OpenSilex base directory to override logger configuration
        File logProfileConfigFile = setup.getBaseDirectory().resolve("logback-" + setup.getProfileId() + ".xml").toFile();
        loadLoggerConfig(logProfileConfigFile, false);

        // If debug flag, set default log level output to DEBUG
        if (setup.isDebug()) {
            ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
            root.setLevel(Level.DEBUG);
            LogFilter.forceDebug();
        }

        LOGGER.debug("Creating OpenSilex instance");
        LOGGER.debug("Base directory:" + setup.getBaseDirectory().toFile().getAbsolutePath());
        LOGGER.debug("Configuration profile: " + setup.getProfileId());

        if (!setup.hasConfigFile()) {
            LOGGER.debug("No main config file defined");
        } else {
            LOGGER.debug("Main config file: " + setup.getConfigFile().getAbsolutePath());
        }

        LOGGER.debug("Create configuration manager");
        ConfigManager cfgManager = new ConfigManager();
        OpenSilexConfig sysConfig = cfgManager.buildSystemConfig(setup.getConfigFile());

        LOGGER.debug("Create modules manager");
        DependencyManager dependencyManager = new DependencyManager(
                ClassUtils.getPomFile(OpenSilex.class,
                        "org.opensilex", "opensilex-main")
        );
        OpenSilexModuleManager modManager = new OpenSilexModuleManager(dependencyManager, setup.getBaseDirectory(), sysConfig);

        if (LOGGER.isDebugEnabled()) {
            modManager.forEachModule(m -> {
                LOGGER.debug("Found module: " + m.getClass().getCanonicalName());
            });
        }

        ServiceManager srvManager = new ServiceManager();

        LOGGER.debug("Create instance");
        OpenSilex app = new OpenSilex(
                modManager,
                cfgManager,
                srvManager,
                sysConfig,
                setup
        );

        return app;
    }

    /**
     * Return default base directory corresponding to Paths.get(System.getProperty("user.dir").
     *
     * @return Default base directory
     */
    public static Path getDefaultBaseDirectory() {
        return Paths.get(System.getProperty("user.dir"));
    }

    /**
     * <pre>
     * Method to override application logback logger configuration.
     * if not reset flag merge configuration file with previously existing configuration
     * otherwise override it.
     *
     * See the following link for logback xml file manual and examples:
     * https://logback.qos.ch/manual/configuration.html
     * https://www.mkyong.com/logging/logback-xml-example/
     * </pre>
     *
     * @param logConfigFile Logback configuration file to merge or override
     * @param reset Flag to determine if configuration must be merge or erase existing
     * @return true if given file has been loaded and false otherwise
     */
    public static boolean loadLoggerConfig(File logConfigFile, boolean reset) {
        // Check if config file exists
        if (logConfigFile.isFile()) {
            try {
                // Reset logger configuration if needed
                LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
                if (reset) {
                    loggerContext.reset();
                }

                // Load new logger configuration file
                JoranConfigurator configurator = new JoranConfigurator();
                configurator.setContext(loggerContext);
                try (InputStream configStream = FileUtils.openInputStream(logConfigFile)) {
                    configurator.doConfigure(configStream);
                    return true;
                }
            } catch (JoranException | IOException ex) {
                LOGGER.warn("Error while trying to load logback configuration file: " + logConfigFile.getAbsolutePath(), ex);
            }
        } else {
            LOGGER.debug("Logger configuration file does not exists: " + logConfigFile.getAbsolutePath());
        }

        return false;
    }

    /**
     * Application configuration manager.
     */
    private final ConfigManager configManager;

    /**
     * Application modules manager.
     */
    private final OpenSilexModuleManager moduleManager;

    /**
     * Application services manager.
     */
    private final ServiceManager serviceManager;

    /**
     * Debug flag.
     */
    private final OpenSilexSetup setup;

    /**
     * Application system configuration.
     */
    private final OpenSilexConfig systemConfig;

    /**
     * Constructor for OpenSilex application.
     *
     * @param moduleManager Module manager
     * @param configManager Configuration manager
     * @param serviceManager Service manager
     * @param systemConfig System configuration
     * @param setup OpenSilex instance configuration
     * @throws Exception In case of construction problem
     */
    private OpenSilex(
            OpenSilexModuleManager moduleManager,
            ConfigManager configManager,
            ServiceManager serviceManager,
            OpenSilexConfig systemConfig,
            OpenSilexSetup setup
    ) throws Exception {
        this.configManager = configManager;
        this.moduleManager = moduleManager;
        this.serviceManager = serviceManager;
        this.systemConfig = systemConfig;
        this.setup = setup;
    }

    /**
     * Application startup method.
     *
     * @throws Exception
     */
    public void startup() throws Exception {
        LOGGER.debug("Starting instance");
        // Add hook to clean modules on shutdown
        if (shutdownHook != null) {
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
        }
        shutdownHook = new Thread() {
            @Override
            public void run() {
                try {
                    shutdown();
                } catch (Exception ex) {
                    LOGGER.error("Error while shutting down opensilex", ex);
                }
            }
        };
        Runtime.getRuntime().addShutdownHook(shutdownHook);

        LOGGER.debug("Initialize modules");
        for (OpenSilexModule module : getModules()) {
            module.setOpenSilex(this);
        }

        setup();

        LOGGER.debug("Setup Services");
        for (Service service : serviceManager.getServices().values()) {
            service.setOpenSilex(this);
            service.setup();
        }

        LOGGER.debug("Start services");
        for (Service service : serviceManager.getServices().values()) {
            service.startup();
        }

        for (OpenSilexModule module : getModules()) {
            module.startup();
        }
        LOGGER.debug("Instance started");
    }

    /**
     * Shutdown application.
     *
     * @throws Exception
     */
    public void shutdown() throws Exception {
        LOGGER.debug("Shutdown instance");

        for (OpenSilexModule module : getModules()) {
            module.shutdown();
        }

        for (Service service : serviceManager.getServices().values()) {
            service.shutdown();
        }

        // Clean all modules
        clean();

    }

    /**
     * Give public access to module iterator.
     *
     * @return Iterator on modules
     */
    public Iterable<OpenSilexModule> getModules() {
        return moduleManager.getModules();
    }

    /**
     * Return module instance corresponding to the given class Throw an exception if the corresponding module is not
     * find.
     *
     * @param <T> The module class of returned instance
     * @param moduleClass The module class from which the instance should be returned
     * @return The module class instance
     * @throws OpenSilexModuleNotFoundException If the corresponding module is not found
     */
    public <T extends OpenSilexModule> T getModuleByClass(Class<T> moduleClass) throws OpenSilexModuleNotFoundException {
        return moduleManager.getModuleByClass(moduleClass);
    }

    /**
     * Return all modules defined in the provided project id (Maven artifact id).
     *
     * @param projectId The maven artifact id from which
     * @return List of module instances contained in given project
     */
    public List<OpenSilexModule> getModulesByProjectId(String projectId) {
        List<OpenSilexModule> modules = new ArrayList<>();
        moduleManager.forEachModule((OpenSilexModule m) -> {
            String moduleClass = ClassUtils.getProjectIdFromClass(m.getClass());
            if (moduleClass.equals(projectId)) {
                modules.add(m);
            }
        });

        return modules;
    }

    /**
     * Return all modules implementing the given extension interface Usefull to create new extension for modules.
     *
     * @param <T> The module extension interface to return
     * @param extensionInterface The module extension interface class
     * @return List of modules implementing the given extension interface
     */
    public <T> List<T> getModulesImplementingInterface(Class<T> extensionInterface) {
        return moduleManager.getModulesImplementingInterface(extensionInterface);
    }

    /**
     * Allow other module to map global loaded YAML config key with an interface.
     *
     * @param <T> The config interface
     * @param key The root key in YAML config
     * @param configClass The interface which map to the configuration structure
     *
     * @return An instance of a class which contains all the loaded configuration
     */
    public <T> T loadConfig(String key, Class<T> configClass) {
        T config = configManager.loadConfig(key, configClass);

        return config;
    }

    /**
     * Allow to directly get a configuration path in the loaded configuration.
     *
     * <pre>
     * Path is determined by the different imbrication level in the
     * configuration separaed with ".". By example RDF4J service configuration is accessible by "opensilex.sparql.rdf4j". it map the following
     * structure in the yaml file:
     *
     * opensilex:
     *      sparql:
     *          rdf4j:
     *              property1: ...
     *              property2: ...
     *              property3: ...
     *              property4: ...
     * </pre>
     *
     * @param <T> The config interface
     * @param path The yaml path to the configration like: "opensilex.sparql.rdf4j"
     * @param configClass The interface which map to the configuration structure
     * @return An instance of a class which contains all the loaded configuration
     */
    public <T> T loadConfigPath(String path, Class<T> configClass) {
        T config = configManager.loadConfigPath(path, configClass);

        return config;
    }

    /**
     * Return application service manager.
     *
     * @return Application service manager
     */
    public ServiceManager getServiceManager() {
        return serviceManager;
    }

    /**
     * Determine application profile identifier.
     *
     * @return Application profile identifier
     */
    public String getProfileId() {
        return setup.getProfileId();
    }

    /**
     * Determine if maven build profile is prod.
     *
     * @return true if maven profile is prod and false otherwise
     */
    public boolean isProd() {
        return getProfileId().equals(PROD_PROFILE_ID);
    }

    /**
     * Determine if maven build profile is test.
     *
     * @return true if maven profile is test and false otherwise
     */
    public boolean isTest() {
        return getProfileId().equals(TEST_PROFILE_ID);
    }

    /**
     * Determine if maven build profile is dev.
     *
     * @return true if maven profile is dev and false otherwise
     */
    public boolean isDev() {
        return getProfileId().equals(DEV_PROFILE_ID);
    }

    /**
     * Determine if application is in debug mode.
     *
     * @return true if application is in debug mode and false otherwise
     */
    public boolean isDebug() {
        return setup.isDebug();
    }

    /**
     * Determine if maven build profile is build.
     *
     * @return true if maven profile is build and false otherwise
     */
    public boolean isReservedProfile(){
        return getProfileId().equals(INTERNAL_OPERATIONS_PROFILE_ID);
    }

    /**
     * Return instance of a service by it's name and interface.
     *
     * @param <T> Service interface
     * @param serviceId service naùe
     * @param serviceInterface Service interface
     * @return Service instance
     */
    public <T extends Service> T getServiceInstance(String serviceId, Class<T> serviceInterface) {
        return serviceManager.getServiceInstance(serviceId, serviceInterface);
    }

    /**
     * Return the base application directory path.
     *
     * @return base application directory path
     */
    public Path getBaseDirectory() {
        return setup.getBaseDirectory();
    }

    /**
     * Return module configuration.
     *
     * @param <T> Module config class
     * @param moduleClass Module class
     * @param configClass Corresponding module configuration class
     * @return Module configuration class instance
     * @throws OpenSilexModuleNotFoundException in case module class is invalid
     */
    public <T> T getModuleConfig(Class<? extends OpenSilexModule> moduleClass, Class<T> configClass) throws OpenSilexModuleNotFoundException {
        return getModuleByClass(moduleClass).getConfig(configClass);
    }

    /**
     * Return application class loader.
     *
     * @return Application class laoder
     */
    public static ClassLoader getClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

    /**
     * Load resource stream from class path.
     *
     * @param name Name of the resource path
     * @return Corresponding resource input stream
     */
    public static InputStream getResourceAsStream(String name) {
        return getClassLoader().getResourceAsStream(name);
    }

    /**
     * Get application setup remaining args (not used for OpenSilex instance setup).
     *
     * @return List of remainging arguments (command parameters)
     */
    public String[] getRemainingArgs() {
        return setup.getRemainingArgs();
    }

    /**
     * Retur raw application configuration files.
     *
     * @return raw configuration file
     */
    public File getConfigFile() {
        return setup.getConfigFile();
    }

    /**
     * Return application default language.
     *
     * @return Default language
     */
    public String getDefaultLanguage() {
        return systemConfig.defaultLanguage();
    }

    /**
     * Return application system configuration.
     *
     * @return system configuration
     */
    public OpenSilexConfig getSystemConfig() {
        return systemConfig;
    }

    /**
     * Return application full processed YAML config.
     *
     * @return Application YAML config string
     * @throws Exception in case of invalid configuration
     */
    public String getExpandedYAMLConfig() throws Exception {
        return configManager.getExpandedYAMLConfig(getSystemConfig(), getModules());
    }

    /**
     * Helper method to get annoted class list in application.
     *
     * @param annotation Annotation to look at
     * @return Map of found annotated classes indexed by name
     */
    public Map<String, Class<?>> getAnnotatedClassesMap(Class<? extends Annotation> annotation) {
        return getAnnotatedClassesMap(annotation, getReflections());
    }

    /**
     * Helper method to get annoted class list in application using a specific reflections instance.
     *
     * @param annotation Annotation to look at
     * @param ref Reflections instance
     * @return Map of found annotated classes indexed by name
     */
    public static Map<String, Class<?>> getAnnotatedClassesMap(Class<? extends Annotation> annotation, Reflections ref) {
        Map<String, Class<?>> classMap = new HashMap<>();

        ref.getTypesAnnotatedWith(annotation).forEach((Class<?> c) -> {
            classMap.put(c.getCanonicalName(), c);
        });

        return classMap;
    }

    /**
     * Helper method to get annoted class list in application.
     *
     * @param annotation Annotation to look at
     * @return List of classes
     */
    public Set<Class<?>> getAnnotatedClasses(Class<? extends Annotation> annotation) {
        return getReflections().getTypesAnnotatedWith(annotation);
    }

    /**
     * Helper method to get annoted method list in application.
     *
     * @param annotation Annotation to look at
     * @return List of methods
     */
    public Set<Method> getMethodsAnnotatedWith(Class<? extends Annotation> annotation) {
        return getReflections().getMethodsAnnotatedWith(annotation);
    }

    /**
     * Entry point to install or re-install all modules.
     *
     * @param reset If true allow module to clear existing installed features before reinstalling
     * @throws Exception In case of installation error
     */
    public void install(boolean reset) throws Exception {
        for (OpenSilexModule module : getModules()) {
            try {
                LOGGER.info("Install module: " + module.getClass().getCanonicalName());
                module.install(reset);
            } catch (Exception ex) {
                LOGGER.error("Fail to install module: " + module.getClass().getCanonicalName(), ex);
                throw ex;
            }
        }
    }

    /**
     * Entry point to check all modules.
     *
     * @throws Exception In case of checking error
     */
    public void check() throws Exception {
        for (OpenSilexModule module : getModules()) {
            try {
                LOGGER.info("Check module: " + module.getClass().getCanonicalName());
                module.check();
            } catch (Exception ex) {
                LOGGER.error("Fail to check module: " + module.getClass().getCanonicalName(), ex);
                throw ex;
            }
        }
    }

    /**
     * Entry point to setup all modules.
     *
     * @throws Exception In case of module setup error
     */
    public void setup() throws Exception {
        for (OpenSilexModule module : getModules()) {
            try {
                LOGGER.debug("Setup module: " + module.getClass().getCanonicalName());
                module.setup();
            } catch (Exception ex) {
                LOGGER.error("Fail to setup module: " + module.getClass().getCanonicalName(), ex);
                throw ex;
            }
        }
    }

    /**
     * Entry point to clean all modules.
     *
     * @throws Exception In case of module cleaning error
     */
    public void clean() throws Exception {
        for (OpenSilexModule module : getModules()) {
            try {
                LOGGER.debug("Clean module: " + module.getClass().getCanonicalName());
                module.clean();
            } catch (Exception ex) {
                LOGGER.error("Fail to clean module: " + module.getClass().getCanonicalName(), ex);
                throw ex;
            }
        }
    }

    /**
     * Internal reflections instance to analyse all application classes and methods.
     */
    private Reflections reflections;

    /**
     * Return reflection instance, building it if needed.
     *
     * @return Reflections instance
     */
    public Reflections getReflections() {
        if (reflections == null) {
            this.buildReflections();
        }
        return reflections;
    }

    /**
     * Build reflection instance by adding all modules JAR to class loader and initialize Reflections library with them.
     */
    private void buildReflections() {
        LOGGER.debug("Initialize JAR URLs to scan by reflection");
        Set<URL> urlsToScan = this.moduleManager.getModulesURLs();

        LOGGER.debug("Exclude ignored modules from reflection");
        Collection<String> ignoredModuleFilePatterns = systemConfig.ignoredModules().values();
        urlsToScan = urlsToScan.stream().filter((URL url) -> {
            for (String ignoredModuleFilePattern : ignoredModuleFilePatterns) {
                if (!url.getPath().endsWith(ignoredModuleFilePattern)) {
                    return true;
                }
            }

            return false;
        }).collect(Collectors.toSet());

        LOGGER.debug("Extra module JAR files registring for static instance");
        Set<URL> jarModulesURLs = new HashSet<>();
        getModules().forEach(m -> {
            File jarFile = ClassUtils.getJarFile(m.getClass());

            if (!m.getClass().equals(ServerModule.class) || !jarFile.isFile()) {

                try {
                    URL jarURL = new URL("file://" + jarFile.getAbsolutePath());
                    LOGGER.debug("Register module JAR URL for" + m.getClass().getSimpleName() + ": " + jarURL.getPath());
                    jarModulesURLs.add(jarURL);
                } catch (MalformedURLException ex) {
                    LOGGER.warn("Invalid module URL for: " + m.getClass().getSimpleName(), ex);
                }
            }
        });

        urlsToScan.addAll(jarModulesURLs);

        ConfigurationBuilder builder;
        if (!urlsToScan.isEmpty()) {

            // Load dependencies through URL Class Loader based on actual class loader
            if (urlsToScan.size() > 0) {
                URLClassLoader classLoader = new URLClassLoader(
                        urlsToScan.toArray(new URL[urlsToScan.size()]),
                        Thread.currentThread().getContextClassLoader()
                );
                LOGGER.debug("Module registred, jar URLs added to classpath");

                // Set the newly created class loader as the main one
                Thread.currentThread().setContextClassLoader(classLoader);
            } else {
                LOGGER.debug("No external module found !");
            }

            builder = ConfigurationBuilder.build("", OpenSilex.getClassLoader())
                    .setUrls(urlsToScan)
                    .setScanners(new TypeAnnotationsScanner(), new SubTypesScanner(), new MethodAnnotationsScanner())
                    .setExpandSuperTypes(false);
        } else {
            builder = ConfigurationBuilder.build("", OpenSilex.getClassLoader())
                    .setScanners(new TypeAnnotationsScanner(), new SubTypesScanner(), new MethodAnnotationsScanner())
                    .setExpandSuperTypes(false);
        }

        reflections = new Reflections(builder);
    }

}

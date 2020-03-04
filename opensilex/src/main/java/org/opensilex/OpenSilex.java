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
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.opensilex.config.ConfigManager;
import org.opensilex.module.ModuleConfig;
import org.opensilex.module.ModuleManager;
import org.opensilex.module.ModuleNotFoundException;
import org.opensilex.dependencies.DependencyManager;
import org.opensilex.service.Service;
import org.opensilex.service.ServiceManager;
import org.opensilex.utils.ClassUtils;
import org.opensilex.utils.LogFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <pre>
 * This is the main class for OpenSilex Application
 *
 * It's a configurable and extensible module system.
 * This class use the singleton pattern.
 * After initialization with a directory, a main config file and a profile identifier,
 * it loads all jar files in "modules" subfolder, and then it initialize all existing modules
 * in application classpath with their own configuration and services.
 *
 * see: OpenSilex.setup static method for more details on initialization mechanism  *
 * Notes:
 * Module management code is delegate to org.opensilex.module.ModuleManager
 * Class Configuration management code is delegate to org.opensilex.config.ConfigManager
 * Class Service management code is delegate to org.opensilex.service.ServiceManager
 * </pre>
 *
 * @author Vincent Migot
 */
public class OpenSilex {

    private final static Logger LOGGER = LoggerFactory.getLogger(OpenSilex.class);

    public final static String DEFAULT_LANGUAGE = "en-US";

    /**
     * Production profile identifier
     */
    public final static String PROD_PROFILE_ID = "prod";

    /**
     * Test profile identifier
     */
    public final static String TEST_PROFILE_ID = "test";

    /**
     * Development profile identifier
     */
    public final static String DEV_PROFILE_ID = "dev";

    /**
     * Singleton variable of the OpenSilex running instance
     */
    private static OpenSilex instance;

    /**
     * Environment key for OpenSilex base directory
     */
    public final static String BASE_DIR_ENV_KEY = "OPENSILEX_BASE_DIRECTORY";

    /**
     * Environment key for OpenSilex profile identifier
     */
    public final static String PROFILE_ID_ENV_KEY = "OPENSILEX_PROFILE_ID";

    /**
     * Environment key for OpenSilex main configuration file
     */
    public final static String CONFIG_FILE_ENV_KEY = "OPENSILEX_CONFIG_FILE";

    /**
     * Command line argument key for OpenSilex base directory
     */
    public final static String BASE_DIR_ARG_KEY = "BASE_DIRECTORY";

    /**
     * Command line argument key for OpenSilex profile identifier
     */
    public final static String PROFILE_ID_ARG_KEY = "PROFILE_ID";

    /**
     * Command line argument key for OpenSilex configuration file
     */
    public final static String CONFIG_FILE_ARG_KEY = "CONFIG_FILE";

    /**
     * Command line argument key for OpenSilex debug flag
     */
    public final static String DEBUG_ARG_KEY = "DEBUG";

    /**
     * Store startup args for reset
     */
    private static String[] SETUP_ARGS;

    /**
     * Store reference to shutdown hook to avoid duplication on reset
     */
    private static Thread SHUTDOWN_HOOK;

    /**
     * <pre>
     * Main method to setup Opensilex instance based on command line arguments,
     * using the following algorithm:
     *
     * - Define Base directory: Set by default with environment variable
     * "OPENSILEX_BASE_DIRECTORY" If "args" array contains "BASE_DIRECTORY"
     * parameter override base directory value If none of these values are
     * defined use java system property "user.dir" corresponding to current user
     * working directory see:
     * https://docs.oracle.com/javase/tutorial/essential/environment/sysprop.html
     *
     * - Define Profile identifier Set by default with environment variable
     * "OPENSILEX_PROFILE_ID" If "args" array contains "PROFILE_ID" parameter
     * override profile identifier value If none of these values are defined use
     * "prod" as default
     *
     * - Define Configuration file Set by default with environment variable
     * "OPENSILEX_CONFIG_FILE" If "args" array contains "_CONFIG_FILE" parameter
     * override config file value Otherwise use only application default
     * configuration including in sources depending of profile
     *
     * - Create OpenSilex instance (see: OpenSilex.createInstance static method
     * for more details) Try to find logback.xml file in OpenSilex base
     * directory to initialize logger configuration Try to find
     * logback-{profile-id}.xml file in OpenSilex base directory to override
     * logger configuration Create OpenSilex object with previously defined
     * parameters
     *
     * - Initialize OpenSilex instance (see: OpenSilex.init method for more
     * details) Create instance for all existing module classes Give reference
     * to OpenSilex instance to all modules Load configuration for all modules
     * found Load services for all modules defined in there configuration Call
     * "init" method for all modules Setup call to "clean" method for all
     * modules at shutdown
     *
     * - Returns all remaining arguments for cli execution
     * </pre>
     *
     * @param args Command line arguments array
     * @return The command line arguments array without the Opensilex parameters
     */
    public static String[] setup(String[] args) {
        SETUP_ARGS = args;

        // Init remaining arguments list to return
        List<String> cliArgsList = new ArrayList<>();

        // Initialize with existing environment variables
        String baseDirectory = System.getenv(BASE_DIR_ENV_KEY);
        String configFile = System.getenv(CONFIG_FILE_ENV_KEY);
        String profileId = System.getenv(PROFILE_ID_ENV_KEY);

        boolean debug = false;

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
            } else if (arg.startsWith("--" + DEBUG_ARG_KEY) && !arg.equalsIgnoreCase("--DEBUG=false")) {
                // For configuration file
                debug = true;

            } else {
                // Otherwise add argument to the remaining list
                cliArgsList.add(arg);
            }
        }

        // Set default value for base directory if not set previously
        if (baseDirectory == null || baseDirectory.equals("")) {
            baseDirectory = System.getProperty("user.dir");
        }

        // Set default profile identifier if not set previously
        if (profileId == null) {
            profileId = PROD_PROFILE_ID;
        }
        try {
            // Create OpenSilex instance with these values depending if configuration file is defined or not.
            if (configFile == null || configFile.equals("")) {
                instance = createInstance(Paths.get(baseDirectory), profileId, null, debug);
            } else {
                instance = createInstance(Paths.get(baseDirectory), profileId, Paths.get(configFile).toFile(), debug);
            }

            LOGGER.debug("Initialize instance");
            instance.init();
            LOGGER.debug("Instance initialized");
        } catch (Exception ex) {
            LOGGER.error("Fail to create and initialize OpenSILEX instance", ex);
        }

        // Return remaining arguments list
        return cliArgsList.toArray(new String[0]);
    }

    /**
     * <pre>
     * Helper method to call easily setup method programatically this way:
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
     */
    public static String[] setup(Map<String, String> args) {
        List<String> argsList = new ArrayList<>();
        args.forEach((String key, String value) -> {
            argsList.add("--" + key + "=" + value);
        });

        return setup(argsList.toArray(new String[0]));
    }

    /**
     * Return OpenSilex singleton instance
     *
     * @return Return OpenSilex singleton instance
     */
    public static OpenSilex getInstance() {
        return instance;
    }

    /**
     * Create and initialize OpenSilex application instance and return it
     *
     * @param baseDirectory Application base directory
     * @param profileId Application profile identifier
     * @param configFile Application main configuration file
     *
     * @return An instance of OpenSilex
     */
    private static OpenSilex createInstance(Path baseDirectory, String profileId, File configFile, boolean debug) throws Exception {

        // Try to find logback.xml file in OpenSilex base directory to initialize logger configuration
        File logConfigFile = baseDirectory.resolve("logback.xml").toFile();
        loadLoggerConfig(logConfigFile, true);

        // Try to find logback-{profile-id}.xml file in OpenSilex base directory to override logger configuration
        File logProfileConfigFile = baseDirectory.resolve("logback-" + profileId + ".xml").toFile();
        loadLoggerConfig(logProfileConfigFile, false);

        // If debug flag, set default log level output to DEBUG
        if (debug) {
            ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
            root.setLevel(Level.DEBUG);
            LogFilter.forceDebug();
        }

        LOGGER.debug("Creating OpenSilex instance");
        LOGGER.debug("Base directory:" + baseDirectory.toFile().getAbsolutePath());
        LOGGER.debug("Configuration profile: " + profileId);

        if (configFile == null) {
            LOGGER.debug("No main config file defined");
        } else {
            LOGGER.debug("Main config file: " + configFile.getAbsolutePath());
        }

        LOGGER.debug("Create instance");
        OpenSilex app = new OpenSilex(baseDirectory, profileId, configFile, new ModuleManager(), new ConfigManager(), new ServiceManager(), debug);

        return app;
    }

    /**
     * <pre>
     * Method to override application logback logger configuration,
     * if not reset flag merge configuration file with previously existing configuration
     * otherwise override it.
     *
     * See the following link for logback xml file manual and examples:
     * https://logback.qos.ch/manual/configuration.html
     * https://www.mkyong.com/logging/logback-xml-example/
     * </pre>
     *
     * @param logConfigFile Logback configuration file to merge or override
     * @param reset Flag to determine if configuration must be merge or erase
     * existing
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
     * Application configuration manager
     */
    private final ConfigManager configManager;

    /**
     * Application modules manager
     */
    private final ModuleManager moduleManager;

    /**
     * Application services manager
     */
    private final ServiceManager serviceManager;

    /**
     * Application profile id
     */
    private final String profileId;

    /**
     * Application base directory
     */
    private final Path baseDirectory;

    /**
     * Application main configuration file
     */
    private final File configFile;

    /**
     * Debug flag
     */
    private final boolean debug;

    /**
     * Constructor for OpenSilex application
     *
     * @param baseDirectory Base directory for the application used to load
     * modules and default configuration files
     * @param profileId Application profile identifier
     * @param configFile Application main configuration file (may be null)
     * @param moduleManager Modules Manager instance
     * @param configManager Configuration Manager instance
     * @param serviceManager Services Manager instance
     */
    private OpenSilex(
            Path baseDirectory,
            String profileId,
            File configFile,
            ModuleManager moduleManager,
            ConfigManager configManager,
            ServiceManager serviceManager,
            boolean debug
    ) {
        this.baseDirectory = baseDirectory;
        this.profileId = profileId;
        this.configFile = configFile;

        this.configManager = configManager;
        this.moduleManager = moduleManager;
        this.serviceManager = serviceManager;

        this.debug = debug;
    }
    
    /**
     * <pre>
     * Initialize application
     *
     * Load all modules and their configuration
     * Load services and initialize them
     * Setup cleaning call for modules on application shutdown
     * </pre>
     */
    private void init() throws Exception {
        LOGGER.debug("Load modules with dependencies");
        DependencyManager dependencyManager = new DependencyManager(
                ClassUtils.getPomFile(OpenSilex.class, "org.opensilex", "opensilex")
        );
        moduleManager.loadModulesWithDependencies(dependencyManager, baseDirectory);

        moduleManager.getModules().forEach(module -> {
            OpenSilex.registerModule(module.getClass());
        });

        LOGGER.debug("Build global configuration");
        configManager.build(baseDirectory, moduleManager.getModules(), profileId, configFile);

        LOGGER.debug("Load modules configuration");
        moduleManager.loadConfigs(configManager);

        LOGGER.debug("Load modules services");
        moduleManager.registerServices(serviceManager);

        // Add hook to clean modules on shutdown
        if (SHUTDOWN_HOOK != null) {
            Runtime.getRuntime().removeShutdownHook(SHUTDOWN_HOOK);
        }
        SHUTDOWN_HOOK = new Thread() {
            @Override
            public void run() {
                try {
                    shutdown();
                } catch (Exception ex) {
                    LOGGER.error("Error while shutting down opensilex", ex);
                }
            }
        };
        Runtime.getRuntime().addShutdownHook(SHUTDOWN_HOOK);

        LOGGER.debug("Startup modules");
        moduleManager.startup();
    }

    /**
     * Restart application
     *
     * @throws Exception
     */
    public void restart() throws Exception {
        shutdown();
        setup(SETUP_ARGS);
    }

    /**
     * Shutdown application
     *
     * @throws Exception
     */
    public void shutdown() throws Exception {
        LOGGER.debug("Shutdown modules");
        moduleManager.shutdown();
    }

    /**
     * Give public access to module iterator
     *
     * @return Iterator on modules
     */
    public Iterable<OpenSilexModule> getModules() {
        return moduleManager.getModules();
    }

    /**
     * Run install method for every modules
     *
     * @throws Exception
     */
    public void install(boolean reset) throws Exception {
        moduleManager.install(reset);
    }
    
    public void check() throws Exception {
        moduleManager.check();
    }
    

    /**
     * Return module instance corresponding to the given class Throw an
     * exception if the corresponding module is not find
     *
     * @param <T> The module class of returned instance
     * @param moduleClass The module class from which the instance should be
     * returned
     * @return The module class instance
     * @throws ModuleNotFoundException If the corresponding module is not found
     */
    public <T extends OpenSilexModule> T getModuleByClass(Class<T> moduleClass) throws ModuleNotFoundException {
        return moduleManager.getModuleByClass(moduleClass);
    }

    /**
     * Return all modules defined in the provided project id (Maven artifact id)
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
     * Return all modules implementing the given extension interface Usefull to
     * create new extension for modules
     *
     * @param <T> The module extension interface to return
     * @param extensionInterface The module extension interface class
     * @return List of modules implementing the given extension interface
     */
    public <T> List<T> getModulesImplementingInterface(Class<T> extensionInterface) {
        return moduleManager.getModulesImplementingInterface(extensionInterface);
    }

    /**
     * Allow other module to map global loaded YAML config key with an interface
     *
     * @param <T> The config interface
     * @param key The root key in YAML config
     * @param configClass The interface which map to the configuration structure
     *
     * @return An instance of a class which contains all the loaded
     * configuration
     */
    public <T> T loadConfig(String key, Class<T> configClass) {
        T config = configManager.loadConfig(key, configClass);

        return config;
    }

    /**
     * Allow to directly get a configuration path in the loaded configuration.
     * Path is determined by the different imbrication level in the
     * configuration separaed with ".". By example RDF4J service configuration
     * is accessible by "opensilex.sparql.rdf4j". it map the following structure
     * in the yaml file:
     * <pre>
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
     * @param path The yaml path to the configration like:
     * "opensilex.sparql.rdf4j"
     * @param configClass The interface which map to the configuration structure
     * @return An instance of a class which contains all the loaded
     * configuration
     */
    public <T> T loadConfigPath(String path, Class<T> configClass) {
        T config = configManager.loadConfigPath(path, configClass);

        return config;
    }

    /**
     * Return application service manager
     *
     * @return Application service manager
     */
    public ServiceManager getServiceManager() {
        return serviceManager;
    }

    /**
     * Determine application profile identifier
     *
     * @return Application profile identifier
     */
    public String getProfileId() {
        return profileId;
    }

    /**
     * Determine if maven build profile is prod
     *
     * @return true if maven profile is prod and false otherwise
     */
    public boolean isProd() {
        return getProfileId().equals(PROD_PROFILE_ID);
    }

    /**
     * Determine if maven build profile is test
     *
     * @return true if maven profile is test and false otherwise
     */
    public boolean isTest() {
        return getProfileId().equals(TEST_PROFILE_ID);
    }

    /**
     * Determine if maven build profile is dev
     *
     * @return true if maven profile is dev and false otherwise
     */
    public boolean isDev() {
        return getProfileId().equals(DEV_PROFILE_ID);
    }

    public boolean isDebug() {
        return debug;
    }

    public static boolean debug() {
        return getInstance().isDebug();
    }

    public <T extends Service> T getServiceInstance(String serviceId, Class<T> serviceInterface) {
        return serviceManager.getServiceInstance(serviceId, serviceInterface);
    }

    /**
     * Return the base application directory path
     *
     * @return base application directory path
     */
    public Path getBaseDirectory() {
        return this.baseDirectory;
    }

    public static <T extends ModuleConfig> T getModuleConfig(Class<? extends OpenSilexModule> moduleClass, Class<T> configClass) throws ModuleNotFoundException {
        return getInstance().getModuleByClass(moduleClass).getConfig(configClass);
    }

    public static ClassLoader getClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

    public static void registerModule(Class<? extends OpenSilexModule> moduleClass) {
        try {
            File f = ClassUtils.getJarFile(moduleClass);
            ClassUtils.addURLToScan(f.toURI().toURL());
        } catch (MalformedURLException ex) {
            LOGGER.warn("Error while adding manually registring a module, should never happend", ex);
        }
    }

    public static InputStream getResourceAsStream(String name) {
        return getClassLoader().getResourceAsStream(name);
    }

    public File getConfigFile() {
        return this.configFile;
    }
}

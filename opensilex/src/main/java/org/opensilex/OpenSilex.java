//******************************************************************************
//                                  OpenSilex.java
// OpenSILEX
// Copyright © INRA 2019
// Creation date: 15 March 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import org.opensilex.module.Module;
import org.opensilex.config.ConfigManager;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.opensilex.module.ModuleManager;
import org.opensilex.service.ServiceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the main class for initializing OpenSilex Application
 */
public class OpenSilex {

    /**
     * Logger
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(OpenSilex.class);

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
     * Return OpenSilex application instance
     *
     * @param baseDirectory Application base directory
     * @param profileId Application profile identifier
     * @param configFile Application main configuration file
     *
     * @return An instance of OpenSilex
     */
    public static OpenSilex getInstance(Path baseDirectory, String profileId, File configFile) {
        if (baseDirectory == null || baseDirectory.toString().equals("")) {
            baseDirectory = getBaseDirectory();
        }

        if (profileId == null) {
            profileId = System.getenv("OPENSILEX_PROFILE_ID");
            if (profileId == null) {
                profileId = PROD_PROFILE_ID;
            }
        }

        if (configFile == null) {
            String cfgPath = System.getenv("OPENSILEX_CONFIG_FILE");
            if (cfgPath != null) {
                configFile = new File(cfgPath);
            }
        }

        File logConfigFile = baseDirectory.resolve("logback.xml").toFile();
        loadLoggerConfig(logConfigFile, true);
        File logProfileConfigFile = baseDirectory.resolve("logback-" + profileId + ".xml").toFile();
        loadLoggerConfig(logProfileConfigFile, false);

        LOGGER.debug("Creating OpenSilex instance");
        LOGGER.debug("Base directory:" + baseDirectory.toFile().getAbsolutePath());
        LOGGER.debug("Configuration profile: " + profileId);

        if (configFile == null) {
            LOGGER.debug("No main config file defined");
        } else {
            LOGGER.debug("Main config file: " + configFile.getAbsolutePath());
        }

        LOGGER.debug("Create instance");
        OpenSilex app = new OpenSilex(baseDirectory, profileId, configFile, new ModuleManager(), new ConfigManager(), new ServiceManager());

        LOGGER.debug("Initialize instance");
        app.init();
        LOGGER.debug("Instance initialized");

        return app;
    }

    public static void loadLoggerConfig(File logConfigFile, boolean reset) {
        if (logConfigFile.isFile()) {
            try {
                LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
                if (reset) {
                    loggerContext.reset();                    
                }
                JoranConfigurator configurator = new JoranConfigurator();
                configurator.setContext(loggerContext);
                InputStream configStream = FileUtils.openInputStream(logConfigFile);
                configurator.doConfigure(configStream); 
                configStream.close();
            } catch (JoranException | IOException ex) {
                LOGGER.warn("Error while trying to load logback configuration file: " + logConfigFile.getAbsolutePath(), ex);
            }
        }
    }
    
    /**
     * Return default base directory ie: Content of "OPENSILEX_DIR" environment
     * variable otherwise constent of system property "user.dir"
     *
     * @return default base directory
     */
    public static Path getBaseDirectory() {
        String directory = System.getenv("OPENSILEX_BASE_DIR");
        if (directory == null) {
            directory = System.getProperty("user.dir");
        }

        return Paths.get(directory);
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
            ServiceManager serviceManager
    ) {
        this.baseDirectory = baseDirectory;
        this.profileId = profileId;
        this.configFile = configFile;

        this.configManager = configManager;
        this.moduleManager = moduleManager;
        this.serviceManager = serviceManager;
    }

    /**
     * Initialize application
     */
    public void init() {
        LOGGER.debug("Load modules with dependencies");
        List<URL> readDependencies = ModuleManager.readDependenciesList(baseDirectory);
        if (readDependencies.size() == 0) {
            List<URL> modulesUrl = ModuleManager.listModulesURLs(baseDirectory);
            List<URL> dependencies = moduleManager.loadModulesWithDependencies(modulesUrl);
            ModuleManager.writeDependenciesList(baseDirectory, dependencies);
        } else {
            moduleManager.registerDependencies(readDependencies);
        }

        LOGGER.debug("Define modules application");
        moduleManager.setApplication(this);

        LOGGER.debug("Build global configuration");
        configManager.build(baseDirectory, moduleManager.getModules(), profileId, configFile);

        LOGGER.debug("Load modules configuration");
        moduleManager.loadConfigs(configManager);

        LOGGER.debug("Load modules services");
        moduleManager.loadServices(serviceManager);

        LOGGER.debug("Initialize modules");
        moduleManager.init();
    }

    public void clean() {
        LOGGER.debug("clean modules");
        moduleManager.clean();
    }

    /**
     * Give public access to module iterator
     *
     * @return Iterator on modules
     */
    public Iterable<Module> getModules() {
        return moduleManager.getModules();
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
}

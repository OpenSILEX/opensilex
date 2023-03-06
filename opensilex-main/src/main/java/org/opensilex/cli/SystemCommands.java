//******************************************************************************
//                          SystemCommands.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2021
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.cli;

import org.apache.commons.lang3.StringUtils;
import org.opensilex.OpenSilex;
import org.opensilex.OpenSilexModule;
import org.opensilex.update.OpenSilexModuleUpdate;
import org.opensilex.utils.AnsiUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;

/**
 * This class regroup all commands concerning OpenSilex system operations.
 *
 * @author Vincent Migot
 * @author rcolin
 */
@Command(
        name = "system",
        header = "Subcommand to group OpenSILEX system operations"
)
public class SystemCommands extends AbstractOpenSilexCommand implements OpenSilexCommand {

    private static final String CLASS_NOT_LOADED_ERROR = "[ERROR] Class [%s] could not be loaded from any OpenSILEX module";
    private static final String RUN_UPDATE_COMMAND_NAME = "run-update";

    private static final Logger LOGGER = LoggerFactory.getLogger(SystemCommands.class);


    private OpenSilexModuleUpdate loadModuleUpdate(String updateClassPath) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {

        if (StringUtils.isEmpty(updateClassPath)) {
            throw new IllegalArgumentException("Null or empty <updateClassName>. Provide a non-empty value for this parameter");
        }

        // try to load class from current ClassLoader
        Class<?> updateClass = null;
        try {
            updateClass = Class.forName(updateClassPath);
        } catch (ClassNotFoundException e) {

            // try to load Class from one OpenSILEX module
            Iterator<OpenSilexModule> moduleIt = getOpenSilex().getModules().iterator();
            while (updateClass == null && moduleIt.hasNext()) {

                // try to load class from the OpenSILEX module ClassLoader
                OpenSilexModule module = moduleIt.next();
                ClassLoader moduleClassLoader = module.getClass().getClassLoader();

                try {
                    updateClass = moduleClassLoader.loadClass(updateClassPath);
                } catch (ClassNotFoundException e2) {
                    // just try to load class from another module
                }
            }
            if (updateClass == null) {
                throw new ClassNotFoundException(String.format(CLASS_NOT_LOADED_ERROR, updateClassPath));
            }
        }
        OpenSilexModuleUpdate update = (OpenSilexModuleUpdate) updateClass.getConstructor().newInstance();
        update.setOpensilex(getOpenSilex());
        return update;
    }

    /**
     * Command to execute an update.
     *
     * @param updateClassPath absolute java path to {@link OpenSilexModuleUpdate} {@link Class} instance (e.g : org.opensilex.MyUpdateClass)
     * @throws OpensilexCommandException If some error is encountered during update execution
     */

    @Command(
            name = RUN_UPDATE_COMMAND_NAME,
            header = "Execute opensilex module specific update",
            hidden = true
    )
    public void runUpdate(
            @Parameters(description = "Update class to execute") String updateClassPath
    ) throws OpensilexCommandException {

        try {
            OpenSilexModuleUpdate update = loadModuleUpdate(updateClassPath);

            AnsiUtils.logDebug(LOGGER, "[{}] Running update {description: {}, date: {} }", updateClassPath, update.getDescription(), update.getDate());
            Instant begin = Instant.now();
            update.execute();

            long elapsedMs = Duration.between(begin, Instant.now()).toMillis();
            AnsiUtils.logDebug(LOGGER, "[{}] Update run [OK] {time: {} ms}", updateClassPath, elapsedMs);

        } catch (Exception e) {
            throw new OpensilexCommandException(this, e);
        }
        finally {
            try {
                getOpenSilex().shutdown();
            } catch (Exception e) {
                AnsiUtils.logError(LOGGER,"Error on OpenSILEX shutdown() : {}", e.getMessage());
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Install or re-install all modules content.
     *
     * @param reset if true re-install instead of install.
     * @throws Exception
     */
    @Command(
            name = "install",
            header = "Install all modules",
            description = "Install or re-install all modules content"
    )
    public void install(
            @CommandLine.Option(names = {"--reset"}, description = "Reset existing databases content", defaultValue = "false") Boolean reset
    ) throws Exception {
        OpenSilex opensilex = getOpenSilex();

        // set OpenSILEX instance for each module (this is performed by default in OpenSilex.startUp() method
        opensilex.getModules().forEach(module -> module.setOpenSilex(opensilex));

        LOGGER.info("Install Modules");
        opensilex.install(reset);
    }

    /**
     * Command to check local installation parameters.
     *
     * @throws Exception
     */
    @Command(
            name = "check",
            header = "Check local installation"
    )
    public void check() throws Exception {
        OpenSilex opensilex = getOpenSilex();

        displayFullConfig();

        LOGGER.debug("Check Modules");
        opensilex.check();
    }

    /**
     * Utility method to launch "check" command in dev mode.
     *
     * @param args unused
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        MainCommand.main(new String[]{
                "system",
                "check",
                "--" + OpenSilex.PROFILE_ID_ARG_KEY + "=" + OpenSilex.DEV_PROFILE_ID
        });
    }

    /**
     * Command to display full processed system configuration.
     *
     * @throws Exception
     */
    @Command(
            name = "full-config",
            header = "Return full configuration"
    )
    public void displayFullConfig() throws Exception {
        OpenSilex opensilex = getOpenSilex();

        LOGGER.debug("Actual expanded configuration");
        LOGGER.info(opensilex.getExpandedYAMLConfig());
    }
}

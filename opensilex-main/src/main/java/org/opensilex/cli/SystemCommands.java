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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.time.Duration;
import java.time.Instant;
import java.util.stream.StreamSupport;

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

    private static final String MODULE_NOT_FOUND_ERROR = "[ERROR] Module [%s] could not be loaded";

    /**
     * Class Logger.
     */
    private static final  Logger LOGGER = LoggerFactory.getLogger(SystemCommands.class);


    /**
     * Command to execute an update.
     *
     * @param updateClassPath absolute java path to {@link OpenSilexModuleUpdate} {@link Class} instance (e.g : org.opensilex.MyUpdateClass)
     * @param moduleName name of an {@link OpenSilexModule} which contains the update class (optional)
     * @throws OpensilexCommandException If some error is encountered during update execution
     */

    @Command(
            name = "run-update",
            header = "Execute opensilex module specific update",
            hidden = true
    )
    public void runUpdate(
            @Parameters(description = "Update class to execute") String updateClassPath,
            @CommandLine.Option(names = {"--module"}, description = "Name of a opensilex module which contains the update class", defaultValue = "") String moduleName
    ) throws OpensilexCommandException {

        try {
            if(StringUtils.isEmpty(updateClassPath)){
                throw new IllegalArgumentException("Null or empty <updateClassName>. Provide a non-empty value for this parameter");
            }

            Class<?> updateClass;

            // try to load class from current ClassLoader
            if (StringUtils.isEmpty(moduleName)) {
                updateClass = Class.forName(updateClassPath);
            } else {
                // search module matching with the given module name, throw exception if no module is found
                OpenSilexModule matchingModule = StreamSupport.stream(getOpenSilex().getModules().spliterator(), false)  // transform iterable to stream
                        .filter(module -> module.getClass().getSimpleName().equals(moduleName))
                        .findAny()
                        .orElseThrow(() -> new IllegalArgumentException(String.format(MODULE_NOT_FOUND_ERROR, moduleName)));

                // load class from the given OpenSILEX module ClassLoader
                ClassLoader moduleClassLoader = matchingModule.getClass().getClassLoader();
                updateClass = moduleClassLoader.loadClass(updateClassPath);
            }

            OpenSilexModuleUpdate updateInstance = (OpenSilexModuleUpdate) updateClass.getConstructor().newInstance();
            updateInstance.setOpensilex(getOpenSilex());

            LOGGER.info("[{}] Running update {description: {}, date: {} }",
                    updateClassPath,
                    updateInstance.getDescription(),
                    updateInstance.getDate()
            );

            Instant begin = Instant.now();
            updateInstance.execute();
            long elapsedMs = Duration.between(begin, Instant.now()).toMillis();
            LOGGER.info("[{}] Update run [OK] {time: {} ms}", updateClassPath, elapsedMs);

        } catch (Exception e) {
            throw new OpensilexCommandException(this, "run-update", e);
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

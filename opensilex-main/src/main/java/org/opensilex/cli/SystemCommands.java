//******************************************************************************
//                          SystemCommands.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.cli;

import org.opensilex.OpenSilex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import org.opensilex.update.OpenSilexModuleUpdate;
import picocli.CommandLine;

/**
 * This class regroup all commands concerning OpenSilex system operations.
 *
 * @author Vincent Migot
 */
@Command(
        name = "system",
        header = "Subcommand to group OpenSILEX system operations"
)
public class SystemCommands extends AbstractOpenSilexCommand implements OpenSilexCommand {

    /**
     * Class Logger.
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(SystemCommands.class);

    /**
     * Command to execute an update.
     *
     * @param updateClassName Update class to execution.
     * @throws Exception
     */
    @Command(
            name = "run-update",
            header = "Execute opensilex module specific update",
            hidden = true
    )
    @SuppressWarnings("unchecked")
    public void runUpdate(
            @Parameters(description = "Update class to execute") String updateClassName
    ) throws Exception {

        Class<?> updateClass = Class.forName(updateClassName, true, OpenSilex.getClassLoader());
        OpenSilexModuleUpdate updateInstance = (OpenSilexModuleUpdate) updateClass.getConstructor().newInstance();
        updateInstance.setOpensilex(getOpenSilex());

        LOGGER.debug("Running update : {class: {}, description: {}, date: {}",
                updateClass.getName(),
                updateInstance.getDescription(),
                updateInstance.getDate()
        );
        updateInstance.execute();
        LOGGER.debug("Update run OK");
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
        System.out.print(opensilex.getExpandedYAMLConfig());
    }
}

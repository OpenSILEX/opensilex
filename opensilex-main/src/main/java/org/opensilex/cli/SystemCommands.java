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
 * <pre>
 * This class regroup all commands concerning OpenSilex system operations:
 * TODO - install: install module(s)
 * TODO - update: Update module(s)
 * TODO - uninstall: uninstall one or all modules
 * TODO - check: check system configuration.
 * TODO Clean and update Javadoc
 * </pre>
 *
 * @author Vincent Migot
 */
@Command(
        name = "system",
        header = "Subcommand to group OpenSILEX system operations"
)
public class SystemCommands extends AbstractOpenSilexCommand implements OpenSilexCommand {

    private final static Logger LOGGER = LoggerFactory.getLogger(SystemCommands.class);

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
        updateInstance.execute();
    }

    @Command(
            name = "install",
            header = "Setup initial database content",
            description = "Setup initial RDF4J and MongoDB database content"
    )
    public void install(
            @CommandLine.Option(names = {"--reset"}, description = "Reset existing databases content", defaultValue = "false") Boolean reset
    ) throws Exception {
        OpenSilex opensilex = getOpenSilex();

        LOGGER.info("Install Modules");
        opensilex.install(reset);
    }

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

    public static void main(String[] args) throws Exception {
        MainCommand.main(new String[]{
            "system",
            "check",
            "--" + OpenSilex.PROFILE_ID_ARG_KEY + "=" + OpenSilex.DEV_PROFILE_ID
        });
    }

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

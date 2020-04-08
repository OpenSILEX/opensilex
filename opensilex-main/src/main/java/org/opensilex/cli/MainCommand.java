//******************************************************************************
//                           MainCommand.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.cli;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import org.opensilex.OpenSilex;
import org.opensilex.OpenSilexModule;
import org.opensilex.OpenSilexSetup;
import static org.opensilex.server.extensions.APIExtension.LOGGER;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.IVersionProvider;
import picocli.CommandLine.Option;

/**
 * This class is the main entry point for the CLI application It uses the
 * Picocli library to automatically generate help messages and argument parsing
 * see: https://picocli.info/
 *
 * @author Vincent Migot
 */
@Command(
        name = "opensilex",
        header = "OpenSILEX Command Line Interface",
        description = "OpenSILEX is an information system based on ontologies"
)
public class MainCommand extends CLIHelpPrinterCommand implements IVersionProvider {

    /**
     * <pre>
     * Version flag option (automatically handled by the picocli library)
     * For details see: https://picocli.info/#_version_help
     * </pre>
     */
    @Option(names = {"-V", "--version"}, versionHelp = true, description = "Print version information and exit.")
    private boolean versionRequested;

    /**
     * Static main to launch commands.
     *
     * @param args Command line arguments array
     */
    public static void main(String[] args) throws Exception {
        // Initialize OpenSilex instance with arguments and return 
        // remaining commands arguments, ie without :
        // - OpenSilex.BASE_DIR_ARG_KEY -> Base directory of the application
        // - OpenSilex.PROFILE_ID --> Laucnh profile of the application
        // - OpenSilex.CONFIG_FILE --> Main configuration file
        // - OpenSilex.DEBUG --> Debug flag
        LOGGER.info("Create OpenSilex instance from command line");
        OpenSilexSetup setup = OpenSilex.createSetup(args);
        for (String s : setup.getRemainingArgs()) {
            LOGGER.warn(s);
        }
        CommandLine cli = getCLI(setup.getRemainingArgs(), null);

        // Avoid to start OpenSilex instance if only help is required
        if (!cli.parseArgs(args).isUsageHelpRequested()) {
            OpenSilex instance = OpenSilex.createInstance(setup);
            commands.forEach((OpenSilexCommand cmd) -> {
                cmd.setOpenSilex(instance);
            });
        }
        cli.execute(setup.getRemainingArgs());
    }

    private static ServiceLoader<OpenSilexCommand> commands;

    public static CommandLine getCLI(String[] args, OpenSilex instance) {
        // If no arguments assume help is requested
        if (args.length == 0) {
            args = new String[]{"--help"};
        }

        // Initialize picocli library
        CommandLine cli = new CommandLine(new MainCommand());

        // Register all commands contained in OpenSilex modules
        commands = ServiceLoader.load(OpenSilexCommand.class, OpenSilex.getClassLoader());

        commands.forEach((OpenSilexCommand cmd) -> {
            if (instance != null) {
                cmd.setOpenSilex(instance);
            }
            Command cmdDef = cmd.getClass().getAnnotation(CommandLine.Command.class);
            cli.addSubcommand(cmdDef.name(), cmd);
        });

        // Define the help factory class
        cli.setHelpFactory(new CLIHelpFactory());

        return cli;
    }

    /**
     * Implementation of picocli.CommandLine.IVersionProvider to display the
     * list of known modules when using the -V command line flag
     *
     * @return List of all module with their version
     * @throws Exception Propagate any exception that could occurs
     */
    @Override
    public String[] getVersion() throws Exception {
        List<String> versionList = new ArrayList<>();

        // Add version in list for all modules
        getOpenSilex().getModules().forEach((OpenSilexModule module) -> {
            versionList.add(module.getClass().getCanonicalName() + ": " + module.getOpenSilexVersion());
        });
        String[] versionListArray = new String[versionList.size()];
        return versionList.toArray(versionListArray);
    }

}

//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.cli;

import java.util.*;
import org.opensilex.*;
import org.opensilex.cli.help.*;
import org.opensilex.module.*;
import picocli.*;
import picocli.CommandLine.*;

/**
 * This class is the main entry point for the CLI application It uses the
 * Picocli library to automatically generate help messages and argument parsing
 * see: https://picocli.info/
 */
@Command(
        name = "opensilex",
        header = "OpenSILEX Command Line Interface",
        description = "OpenSILEX is an information system based on ontologies"
)
public class MainCommand extends HelpPrinterCommand implements IVersionProvider {

    /**
     * Version flag option (automatically handled by the picocli library see:
     * https://picocli.info/#_version_help
     */
    @Option(names = {"-V", "--version"}, versionHelp = true, description = "Print version information and exit.")
    private boolean versionRequested;

    public static void main(String[] args) {
        // Initialize opensilex instance with arguments and return commands arguments
        String[] cliArgs = OpenSilex.setup(args);
        run(cliArgs);
    }

    public static void run(String[] args) {
        // If no arguments assume help is requested
        if (args.length == 0) {
            args = new String[]{"--help"};
        }

        // Initialize picocli library
        CommandLine cli = new CommandLine(new MainCommand());

        // Register all commands contained in OpenSilex modules
        ServiceLoader.load(OpenSilexCommand.class, Thread.currentThread().getContextClassLoader())
                .forEach((OpenSilexCommand cmd) -> {
                    Command cmdDef = cmd.getClass().getAnnotation(CommandLine.Command.class);
                    cli.addSubcommand(cmdDef.name(), cmd);
                });

        // Define the help factory class
        cli.setHelpFactory(new HelpFactory());

        // Run actual commands
        cli.execute(args);
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
        ModuleManager moduleManager = new ModuleManager();
        moduleManager.forEachModule((OpenSilexModule module) -> {
            versionList.add(module.getClass().getCanonicalName() + ": " + module.getOpenSilexVersion());
        });
        String[] versionListArray = new String[versionList.size()];
        return versionList.toArray(versionListArray);
    }

}

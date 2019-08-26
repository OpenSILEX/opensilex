package org.opensilex.cli;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import org.opensilex.OpenSilex;
import org.opensilex.module.OpenSilexModule;
import org.opensilex.cli.help.HelpFactory;
import org.opensilex.cli.help.HelpPrinterCommand;
import org.opensilex.module.ModuleManager;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.IVersionProvider;

/**
 *
 * @author vincent
 */
@Command(
        name = "opensilex",
        header = "OpenSILEX Command Line Interface",
        description = "OpenSILEX is an information system based on ontologies"
)
public class MainCommand extends HelpPrinterCommand implements IVersionProvider {

    /**
     * Version flag option
     */
    @Option(names = {"-V", "--version"}, versionHelp = true, description = "Print version information and exit.")
    private boolean versionRequested;

    private static String[] cliArgs;

    public static void main(String[] args) {
        // Initialize instance with arguments
        cliArgs  = OpenSilex.initWithArgs(args);

        // Initialize Picocli CommandLine
        if (cliArgs.length == 0) {
            cliArgs = new String[]{"--help"};
        }

        CommandLine cli = new CommandLine(new MainCommand());
        ServiceLoader.load(SubCommand.class, Thread.currentThread().getContextClassLoader())
                .forEach((SubCommand cmd) -> {
                    Command cmdDef = cmd.getClass().getAnnotation(CommandLine.Command.class);
                    cli.addSubcommand(cmdDef.name(), cmd);
                });
        cli.setHelpFactory(new HelpFactory());
        cli.execute(cliArgs);
    }

    /**
     * Implementation of picocli.CommandLine.IVersionProvider to display the
     * list of known modules when using the -V command line flag
     *
     * @return
     * @throws Exception
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

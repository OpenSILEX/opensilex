package org.opensilex.cli;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import org.opensilex.OpenSilex;
import org.opensilex.module.Module;
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

    public static void main(String[] args) {
        // Initialize Picocli CommandLine
        if (args.length == 0) {
            args = new String[]{"--help"};
        }
        CommandLine cli = new CommandLine(new MainCommand());
        OpenSilex.getInstance(null, null, null);
        ServiceLoader.load(SubCommand.class, Thread.currentThread().getContextClassLoader())
                .forEach((SubCommand cmd) -> {
                    Command cmdDef = cmd.getClass().getAnnotation(CommandLine.Command.class);
                    cli.addSubcommand(cmdDef.name(), cmd);
                });
        cli.setHelpFactory(new HelpFactory());
        cli.execute(args);
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
        moduleManager.forEachModule((Module module) -> {
            versionList.add(module.getClass().getCanonicalName() + ": " + module.getOpenSilexVersion());
        });
        String[] versionListArray = new String[versionList.size()];
        return versionList.toArray(versionListArray);
    }

}

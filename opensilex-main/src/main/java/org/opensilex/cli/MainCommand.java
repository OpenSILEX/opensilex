//******************************************************************************
//                           MainCommand.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.cli;

import java.io.File;
import java.io.FileReader;
import java.util.*;

import org.opensilex.OpenSilex;
import org.opensilex.OpenSilexModule;
import org.opensilex.OpenSilexSetup;
import static org.opensilex.server.extensions.APIExtension.LOGGER;
import org.opensilex.utils.ClassUtils;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.IVersionProvider;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

/**
 * This class is the main entry point for the CLI application.
 *
 * It uses the Picocli library to automatically generate help messages and argument parsing, see: https://picocli.info/
 *
 * @author Vincent Migot
 */
@Command(
        name = "opensilex",
        header = "OpenSILEX Command Line Interface",
        description = "OpenSILEX is an information system based on ontologies"
)
public class MainCommand extends AbstractOpenSilexCommand implements IVersionProvider {

    public static final String VERSION_COMMAND = "--version";
    public static final String VERSION_ALIAS_COMMAND = "-V";

    /**
     * <pre>
     * Version flag option (automatically handled by the picocli library).
     * For details see: https://picocli.info/#_version_help
     * </pre>
     */
    @Option(names = {VERSION_ALIAS_COMMAND, VERSION_COMMAND}, versionHelp = true, description = "Print version information and exit.")
    private boolean versionRequested;

    /**
     * Static main to launch commands.
     *
     * @param args Command line arguments array
     * @throws Exception In case of any error during command execution
     */
    public static void main(String[] args) throws Exception {
        boolean forceDebug = false;
//        forceDebug = true;

        
        LOGGER.debug("Create OpenSilex instance from command line");
        OpenSilexSetup setup = OpenSilex.createSetup(args, forceDebug);
        OpenSilex instance = OpenSilex.createInstance(setup, false);

        // If no arguments assume help is requested
        args = setup.getRemainingArgs();
        for (String s : args) {
            LOGGER.debug("CLI input parameters : {}", s);
        }

        if (args.length == 0) {
            args = new String[]{HelpOption.HELP_COMMAND};
        }
        
        CommandLine cli = getCLI(args, null);

        try {
            // Avoid to start OpenSilex instance if only help or some basic command is required
            CommandLine.ParseResult parsedArgs = cli.parseArgs(args);

            Set<String> noOpenSilexStartCommands = new HashSet<>();

            // exclude help command
            noOpenSilexStartCommands.add(HelpOption.HELP_COMMAND);
            noOpenSilexStartCommands.add(HelpOption.HELP_ALIAS_COMMAND);

            // exclude version command
            noOpenSilexStartCommands.add(MainCommand.VERSION_COMMAND);
            noOpenSilexStartCommands.add(MainCommand.VERSION_ALIAS_COMMAND);

            // exclude git-commit command
            noOpenSilexStartCommands.add(MainCommand.GIT_COMMIT_COMMAND);

            // #TODO properly handle opensilex start/and profile by command

            boolean launchOpenSilex = false;
            List<CommandLine> foundCommands = parsedArgs.asCommandLineList();

            if (!foundCommands.isEmpty()) {
                CommandLine commandToExecute = foundCommands.get(foundCommands.size() - 1);

                // run OpenSILEX if the command is not a special command or
                // if no subcommands were found
                launchOpenSilex = ! noOpenSilexStartCommands.contains(commandToExecute.getCommandName())
                        || ! commandToExecute.getSubcommands().isEmpty();
            }

            if (launchOpenSilex) {
                instance.startup();
                commands.forEach((OpenSilexCommand cmd) -> {
                    cmd.setOpenSilex(instance);
                });
            }
        } catch (CommandLine.ParameterException ex) {
            // Silently ignore parameter exceptions meaning help will be printed
        }

        cli.execute(args);
    }

    /**
     * Loader for commands.
     */
    private static ServiceLoader<OpenSilexCommand> commands;

    /**
     * Return command line instance.
     *
     * @param args Command line arguments
     * @param instance OpenSilex instance
     *
     * @return loaded command
     */
    public static CommandLine getCLI(String[] args, OpenSilex instance) {

        // Initialize picocli library
        CommandLine cli = new CommandLine(new MainCommand()) {

        };

        // Register all commands contained in OpenSilex modules
        LOGGER.debug("Load commands");
        commands = ServiceLoader.load(OpenSilexCommand.class, OpenSilex.getClassLoader());
        commands.forEach((OpenSilexCommand cmd) -> {
            if (instance != null) {
                cmd.setOpenSilex(instance);
            }
            Command cmdDef = cmd.getClass().getAnnotation(CommandLine.Command.class);
            cli.addSubcommand(cmdDef.name(), cmd);
            LOGGER.debug("Add command: " + cmdDef.name());
        });
        LOGGER.debug("Commands loaded");

        // Define the help factory class
        cli.setHelpFactory(new HelpFactory());

        return cli;
    }

    /**
     * Implementation of picocli.CommandLine.IVersionProvider to display the list of known modules when using the -V command line flag.
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

    public static final String GIT_COMMIT_COMMAND = "git-commit";

    /**
     * Display git commit number used for building this OpenSilex version.
     */
    @Command(
            name = GIT_COMMIT_COMMAND,
            header = "Display git commit",
            description = "Display git commit identifier used for building this OpenSilex version"
    )
    public void gitCommit(
            @Mixin HelpOption help) {
        try {
            File gitPropertiesFile = ClassUtils.getFileFromClassArtifact(OpenSilex.class, "git.properties");

            Properties gitProperties = new Properties();
            gitProperties.load(new FileReader(gitPropertiesFile));

            String gitCommitAbbrev = gitProperties.getProperty("git.commit.id.abbrev", null);
            String gitCommitFull = gitProperties.getProperty("git.commit.id.full", null);
            String gitCommitMessage = gitProperties.getProperty("git.commit.message.full", null);
            String gitCommitUsername = gitProperties.getProperty("git.commit.user.name", null);
            String gitCommitUsermail = gitProperties.getProperty("git.commit.user.email", null);
            if (gitCommitUsername != null && gitCommitUsermail != null) {
                gitCommitUsername = gitCommitUsername + " <" + gitCommitUsermail + ">";
            }

            if (gitCommitAbbrev == null || gitCommitFull == null) {
                System.out.println("No git commit information found");
            } else {
                System.out.println("Git commit id: " + gitCommitAbbrev + " (" + gitCommitFull + ")");
                if (gitCommitMessage != null) {
                    System.out.println("Git commit message: " + gitCommitMessage);
                }
                if (gitCommitUsername != null) {
                    System.out.println("Git commit user: " + gitCommitUsername);
                }
            }
        } catch (Exception ex) {
            System.out.println("No git commit information found");
            LOGGER.debug("Exception raised:", ex);
        }
    }

}

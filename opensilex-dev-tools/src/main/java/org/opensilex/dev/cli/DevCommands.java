/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.dev.cli;

import org.opensilex.dev.Install;
import java.nio.file.Path;
import org.opensilex.OpenSilex;
import org.opensilex.cli.OpenSilexCommand;
import org.opensilex.cli.CLIHelpOption;
import org.opensilex.cli.CLIHelpPrinterCommand;
import org.opensilex.dev.StartServer;
import org.opensilex.dev.StartServerWithFront;
import picocli.CommandLine;

/**
 *
 * @author vmigot
 */
@CommandLine.Command(
        name = "dev",
        header = "Subcommand to group OpenSILEX development tools operations"
)
public class DevCommands extends CLIHelpPrinterCommand implements OpenSilexCommand {

    @CommandLine.Command(
            name = "install",
            header = "Initialize OpenSILEX development databases",
            description = "Initialize or reset OpenSILEX development databases and default admin user"
    )
    public void install(
            @CommandLine.Option(names = {"-r", "--reset"}, description = "Clear existing database before initialization", defaultValue = "false") boolean reset,
            @CommandLine.Mixin CLIHelpOption help
    ) throws Exception {
        Path baseDirectory = OpenSilex.getDefaultBaseDirectory().resolve("../../../opensilex-dev-tools/");
        Install.install(reset, baseDirectory);
    }

    @CommandLine.Command(
            name = "start",
            header = "Start OpenSILEX front development server",
            description = "Start OpenSILEX front development with Vue.JS hot reload or not"
    )
    public void start(
            @CommandLine.Option(names = {"--no-front-dev"}, description = "Disable Vue.JS hot reload development server", defaultValue = "false") boolean noFrontDev,
            @CommandLine.Mixin CLIHelpOption help
    ) throws Exception {
        Path baseDirectory = OpenSilex.getDefaultBaseDirectory().resolve("../../../opensilex-dev-tools/");
        if (noFrontDev) {
            StartServer.start(baseDirectory);
        } else {
            StartServerWithFront.start(baseDirectory);
        }

    }
}

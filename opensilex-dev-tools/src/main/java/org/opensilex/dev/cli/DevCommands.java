/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.dev.cli;

import org.opensilex.dev.Install;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.opensilex.cli.OpenSilexCommand;
import org.opensilex.cli.help.HelpOption;
import org.opensilex.cli.help.HelpPrinterCommand;
import picocli.CommandLine;

/**
 *
 * @author vmigot
 */
@CommandLine.Command(
        name = "dev",
        header = "Subcommand to group OpenSILEX development tools operations"
)
public class DevCommands extends HelpPrinterCommand implements OpenSilexCommand {
    
    @CommandLine.Command(
            name = "install",
            header = "Initialize OpenSILEX development databases",
            description = "Initialize or reset OpenSILEX development databases and default admin user"
    )
    public void start(
            @CommandLine.Option(names = {"-r", "--reset"}, description = "Clear existing database before initialization", defaultValue = "false") boolean reset,
            @CommandLine.Mixin HelpOption help
    ) throws Exception {
        Path baseDirectory =  Paths.get(System.getProperty("user.dir")).resolve("../../../opensilex-dev-tools/");
        Install.install(reset, baseDirectory.toAbsolutePath().toString());
    }
}

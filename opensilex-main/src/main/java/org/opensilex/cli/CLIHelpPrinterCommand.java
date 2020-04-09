//******************************************************************************
//                        CLIHelpPrinterCommand.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.cli;

import org.opensilex.OpenSilex;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Mixin;

/**
 * Utility class used as super class for commands which are only a regroup of
 * other commands and which only has purpose of displaying help for them.
 *
 * @author Vincent Migot
 */
@Command(
        description = "Calling this command will simply display help message with all subcommands",
        subcommands = {
            HelpCommand.class
        },
        headerHeading = CLIHelpFactory.CLI_HEADER_HEADING,
        synopsisHeading = CLIHelpFactory.CLI_SYNOPSYS_HEADING,
        descriptionHeading = CLIHelpFactory.CLI_DESCRIPTION_HEADING,
        parameterListHeading = CLIHelpFactory.CLI_PARAMETER_LIST_HEADING,
        optionListHeading = CLIHelpFactory.CLI_OPTION_LIST_HEADING,
        commandListHeading = CLIHelpFactory.CLI_COMMAND_LIST_HEADING,
        footer = CLIHelpFactory.CLI_FOOTER,
        versionProvider = MainCommand.class
)
public abstract class CLIHelpPrinterCommand implements Runnable, OpenSilexCommand {

    /**
     * Generic help option
     */
    @Mixin
    private CLIHelpOption help = new CLIHelpOption();

    /**
     * Display help if called with no arguments
     */
    @Override
    public void run() {
        CommandLine.usage(this, System.out);
    }

    private OpenSilex opensilex;

    @Override
    public OpenSilex getOpenSilex() {
        return this.opensilex;
    }

    @Override
    public void setOpenSilex(OpenSilex instance) {
        this.opensilex = instance;
    }

}

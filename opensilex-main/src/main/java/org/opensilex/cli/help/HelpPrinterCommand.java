//******************************************************************************
//                        HelpPrinterCommand.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.cli.help;

import org.opensilex.OpenSilex;
import org.opensilex.cli.MainCommand;
import org.opensilex.cli.OpenSilexCommand;
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
        headerHeading = HelpFactory.CLI_HEADER_HEADING,
        synopsisHeading = HelpFactory.CLI_SYNOPSYS_HEADING,
        descriptionHeading = HelpFactory.CLI_DESCRIPTION_HEADING,
        parameterListHeading = HelpFactory.CLI_PARAMETER_LIST_HEADING,
        optionListHeading = HelpFactory.CLI_OPTION_LIST_HEADING,
        commandListHeading = HelpFactory.CLI_COMMAND_LIST_HEADING,
        footer = HelpFactory.CLI_FOOTER,
        versionProvider = MainCommand.class
)
public abstract class HelpPrinterCommand implements Runnable, OpenSilexCommand {

    /**
     * Generic help option
     */
    @Mixin
    private HelpOption help = new HelpOption();

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

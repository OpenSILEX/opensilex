/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.dev.cli;

import org.opensilex.cli.SubCommand;
import org.opensilex.cli.help.HelpPrinterCommand;
import picocli.CommandLine;

/**
 *
 * @author vincent
 */
@CommandLine.Command(
        name = "dev",
        header = "Subcommand to group OpenSILEX development operations"
)
public class DevCommand extends HelpPrinterCommand implements SubCommand{
    
}

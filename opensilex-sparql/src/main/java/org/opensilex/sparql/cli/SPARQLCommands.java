/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql.cli;

import java.net.URI;
import org.opensilex.OpenSilex;
import org.opensilex.cli.OpenSilexCommand;
import org.opensilex.cli.help.HelpOption;
import org.opensilex.cli.help.HelpPrinterCommand;
import org.opensilex.sparql.service.SPARQLService;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/**
 *
 * @author vmigot
 */
@Command(
        name = "sparql",
        header = "Subcommand to group OpenSILEX sparql operations"
)
public class SPARQLCommands extends HelpPrinterCommand implements OpenSilexCommand {

    @CommandLine.Command(
            name = "rename-graph",
            header = "Rename a SPARQL graph",
            description = "Rename the given graph and move all content"
    )
    public void renameGraph(
            @CommandLine.Parameters(description = "Old graph URI", defaultValue = "") URI oldGraphURI,
            @CommandLine.Parameters(description = "New graph URI", defaultValue = "") URI newGraphURI,
            @CommandLine.Mixin HelpOption help
    ) throws Exception {
        SPARQLService sparql = OpenSilex.getInstance().getServiceInstance("sparql", SPARQLService.class);
        sparql.renameGraph(oldGraphURI, newGraphURI);
    }
}

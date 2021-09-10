/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql.cli;

import java.net.URI;
import java.util.List;
import java.util.Map;
import org.opensilex.cli.OpenSilexCommand;
import org.opensilex.cli.HelpOption;
import org.opensilex.cli.AbstractOpenSilexCommand;
import org.opensilex.sparql.SPARQLModule;
import org.opensilex.sparql.exceptions.SPARQLValidationException;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.service.SPARQLServiceFactory;
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
public class SPARQLCommands extends AbstractOpenSilexCommand implements OpenSilexCommand {

    @CommandLine.Command(
            name = "reset-ontologies",
            header = "Reset configured ontologies graph",
            description = "Reset configured ontologies graph defined in each modules"
    )
    public void resetOntologies(
            @CommandLine.Mixin HelpOption help
    ) throws Exception {
        SPARQLServiceFactory factory = getOpenSilex().getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class);
        SPARQLService sparql = factory.provide();
        try {
            getOpenSilex().getModuleByClass(SPARQLModule.class).installOntologies(sparql, true);
        } finally {
            factory.dispose(sparql);
        }
        factory.dispose(sparql);
    }

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
        SPARQLServiceFactory factory = getOpenSilex().getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class);
        SPARQLService sparql = factory.provide();
        sparql.renameGraph(oldGraphURI, newGraphURI);
        factory.dispose(sparql);
    }

    @CommandLine.Command(
            name = "shacl-enable",
            header = "Enable SHACL validation",
            description = "Enable SHACL validation"
    )
    public void shaclEnable(
            @CommandLine.Mixin HelpOption help
    ) throws Exception {
        SPARQLServiceFactory factory = getOpenSilex().getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class);
        SPARQLService sparql = factory.provide();
        try {
            sparql.enableSHACL();
        } catch (SPARQLValidationException ex) {
            System.out.println("Error while enable SHACL validation:");
            System.out.println(ex.getMessage());
            sparql.disableSHACL();
        }
        factory.dispose(sparql);
    }

    @CommandLine.Command(
            name = "shacl-disable",
            header = "Disable SHACL validation",
            description = "Disable SHACL validation"
    )
    public void shaclDisable(
            @CommandLine.Mixin HelpOption help
    ) throws Exception {
        SPARQLServiceFactory factory = getOpenSilex().getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class);
        SPARQLService sparql = factory.provide();
        try {
            sparql.disableSHACL();
        } finally {
            factory.dispose(sparql);
        }
    }
}

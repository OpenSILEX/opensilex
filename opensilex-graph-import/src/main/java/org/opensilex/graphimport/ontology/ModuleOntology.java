//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.ontology;

import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.vocabulary.OWL2;
import org.opensilex.sparql.service.SPARQLService;

import java.io.InputStream;
import java.net.URI;

/**
 * The ontology the module ships: restrictions the assistant needs and oeso-core does not declare, such
 * as "a factor level belongs to exactly one factor".
 * <p>
 * The module loads it itself, into its own graph, when the assistant is used. It is not a
 * {@code SPARQLExtension} on purpose: the platform requires the graph of every such extension at
 * startup, so each instance of the branch would have to run the system setup again. The platform's
 * ontology store reads every graph, so it sees these restrictions from its next start.
 *
 * @author Arnaud Charleroy
 */
public final class ModuleOntology {

    /** The graph the file is loaded into, which is also the IRI of the ontology. */
    public static final URI GRAPH = URI.create("http://www.opensilex.org/vocabulary/oeso-graph-import");

    /** Must match {@code owl:versionInfo} in the file: a new version replaces the graph. */
    static final String VERSION = "4";

    private static final String FILE = "/ontologies/oeso-graph-import.ttl";

    private ModuleOntology() {
    }

    /**
     * Loads the file into its graph unless the graph already holds this version. Asked on every use,
     * since the triplestore may have been emptied since the last one; the question is one ASK query.
     */
    public static synchronized void ensureLoaded(SPARQLService sparql) throws Exception {
        if (isLoaded(sparql)) {
            return;
        }
        sparql.clearGraph(GRAPH);
        try (InputStream file = ModuleOntology.class.getResourceAsStream(FILE)) {
            if (file == null) {
                throw new IllegalStateException("The module's ontology file is missing: " + FILE);
            }
            sparql.loadOntology(GRAPH, file, Lang.TURTLE);
        }
    }

    /** Whether the graph holds the version this code expects. */
    static boolean isLoaded(SPARQLService sparql) throws Exception {
        AskBuilder ask = new AskBuilder().addGraph(NodeFactory.createURI(GRAPH.toString()),
                NodeFactory.createURI(GRAPH.toString()), OWL2.versionInfo, NodeFactory.createLiteral(VERSION));
        return sparql.executeAskQuery(ask);
    }
}

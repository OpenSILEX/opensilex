//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.monitoring.health.stats;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.opensilex.OpenSilex;
import org.opensilex.monitoring.config.HealthConfig;
import org.opensilex.sparql.rdf4j.RDF4JServiceFactory;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.service.SPARQLServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Counts triples through the RDF4J repository rather than through SPARQL.
 *
 * <p>{@code RepositoryConnection.size()} is answered from the store's own statement counter — over
 * HTTP it is a single {@code GET /repositories/{id}/size}. The SPARQL equivalent,
 * {@code SELECT (count(*) AS ?c) WHERE { ?s ?p ?o }}, is a full store scan that occupies an
 * rdf4j-server worker for seconds to minutes on a real instance.</p>
 *
 * <p>The factory is pattern-matched rather than cast: the SPARQL implementation is swappable, and
 * an implementation that is not RDF4J simply reports no count instead of failing.</p>
 *
 * @author Arnaud Charleroy
 */
public class TripleStoreStatsReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(TripleStoreStatsReader.class);

    private final OpenSilex opensilex;
    private final HealthConfig config;

    public TripleStoreStatsReader(OpenSilex opensilex, HealthConfig config) {
        this.opensilex = opensilex;
        this.config = config;
    }

    public TripleStoreVolumetry read() {
        if (!config.tripleCountEnabled()) {
            return new TripleStoreVolumetry(null, null, List.of(), Instant.now(),
                    "triple count disabled by configuration");
        }

        SPARQLServiceFactory factory = opensilex.getServiceInstance(
                SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class);

        if (!(factory instanceof RDF4JServiceFactory rdf4j)) {
            return new TripleStoreVolumetry(null, null, List.of(), Instant.now(),
                    "unsupported triple store implementation: " + factory.getClass().getSimpleName());
        }

        try (RepositoryConnection connection = rdf4j.getRepository().getConnection()) {
            long total = connection.size();
            List<TripleStoreVolumetry.GraphVolumetry> graphs = new ArrayList<>();
            int graphCount = 0;

            try (RepositoryResult<Resource> contexts = connection.getContextIDs()) {
                for (Resource context : contexts) {
                    graphCount++;
                    // One HTTP round trip per graph, hence the flag: a mature instance has one
                    // named graph per experiment and this fans out to hundreds of calls.
                    if (config.tripleCountPerGraph()) {
                        graphs.add(new TripleStoreVolumetry.GraphVolumetry(
                                context.stringValue(), connection.size(context)));
                    }
                }
            }

            graphs.sort((a, b) -> Long.compare(b.tripleCount(), a.tripleCount()));
            return new TripleStoreVolumetry(total, graphCount, graphs, Instant.now(), null);
        } catch (Exception e) {
            LOGGER.warn("Cannot read triple store statistics", e);
            return new TripleStoreVolumetry(null, null, List.of(), Instant.now(),
                    e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }
}

//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.monitoring.health;

import static org.apache.jena.sparql.core.Var.alloc;

import org.apache.jena.arq.querybuilder.AskBuilder;
import org.opensilex.OpenSilex;
import org.opensilex.monitoring.extension.HealthIndicator;
import org.opensilex.monitoring.extension.ProbeResult;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.service.SPARQLServiceFactory;

/**
 * Triple store liveness, through a trivial ASK.
 *
 * <p>The service is obtained from its factory rather than injected: {@code SPARQLService} is bound
 * request-scoped behind an HK2 proxy and is therefore unusable from a probe thread.</p>
 *
 * @author Arnaud Charleroy
 */
public class TripleStoreHealthIndicator implements HealthIndicator {

    public static final String NAME = "rdf4j";

    private final OpenSilex opensilex;

    public TripleStoreHealthIndicator(OpenSilex opensilex) {
        this.opensilex = opensilex;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public ProbeResult probe() throws Exception {
        long start = System.nanoTime();
        SPARQLServiceFactory factory = opensilex.getServiceInstance(
                SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class);
        SPARQLService sparql = factory.provide();
        try {
            sparql.executeAskQuery(new AskBuilder().addWhere(alloc("s"), alloc("p"), alloc("o")));
        } finally {
            factory.dispose(sparql);
        }
        return ProbeResult.up(MongoHealthIndicator.elapsedMs(start));
    }
}

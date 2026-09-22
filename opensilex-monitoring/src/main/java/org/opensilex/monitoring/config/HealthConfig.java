//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.monitoring.config;

import org.opensilex.config.ConfigDescription;

/**
 * Health check tuning. Every probe is bounded in time and every statistic is cached, because a
 * monitoring page is polled and must never become a load source of its own.
 *
 * @author Arnaud Charleroy
 */
public interface HealthConfig {

    @ConfigDescription(
            value = "Hard deadline for a single health probe, in milliseconds. RDF4J is reached over"
                    + " HTTP with no socket timeout of its own, so this is the only thing standing"
                    + " between a frozen triple store and a frozen endpoint.",
            defaultInt = 2000
    )
    int checkTimeoutMs();

    @ConfigDescription(
            value = "Size of the probe thread pool. Caps the damage a probe polling the health"
                    + " endpoint every second can do to the RDF4J connection pool.",
            defaultInt = 2
    )
    int maxConcurrentChecks();

    @ConfigDescription(
            value = "Report the total number of RDF triples (admin only)",
            defaultBoolean = true
    )
    boolean tripleCountEnabled();

    @ConfigDescription(
            value = "How long a triple count stays cached, in minutes. Nobody decides anything on a"
                    + " triple count that changed thirty seconds ago.",
            defaultInt = 15
    )
    int tripleCountRefreshMinutes();

    @ConfigDescription(
            value = "Also count triples per named graph. One HTTP round trip per graph, and a mature"
                    + " instance has hundreds of them, so this is off by default.",
            defaultBoolean = false
    )
    boolean tripleCountPerGraph();

    @ConfigDescription(
            value = "Report MongoDB database volumetry (dbStats)",
            defaultBoolean = true
    )
    boolean mongoDatabaseStats();

    @ConfigDescription(
            value = "Report per-collection MongoDB volumetry",
            defaultBoolean = true
    )
    boolean mongoCollectionStats();

    @ConfigDescription(
            value = "How long MongoDB volumetry stays cached, in seconds",
            defaultInt = 60
    )
    int mongoStatsRefreshSeconds();
}

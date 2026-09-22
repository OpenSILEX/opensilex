//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.monitoring.activity;

import java.time.Instant;
import java.util.List;

/**
 * Turns the raw access log into the series the activity chart draws.
 *
 * <p>An interface with one implementation today, so that where the log lives stays an
 * implementation detail of the module rather than a shape baked into the API.</p>
 *
 * @author Arnaud Charleroy
 */
public interface ActivityAggregator {

    /**
     * @param from        inclusive lower bound
     * @param to          exclusive upper bound
     * @param granularity bucket size
     * @return one entry per non-empty bucket, ordered by time
     */
    List<ActivityBucket> aggregate(Instant from, Instant to, Granularity granularity);

    /**
     * @return the number of distinct accounts over the whole period. This is deliberately a
     *         separate query: it is <em>not</em> the sum of the per-bucket counts, since a user
     *         active on several days would be counted several times.
     */
    long countDistinctUsers(Instant from, Instant to);
}

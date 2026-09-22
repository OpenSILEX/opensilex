//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.monitoring.config;

import org.opensilex.config.ConfigDescription;

/**
 * Activity report tuning.
 *
 * @author Arnaud Charleroy
 */
public interface ActivityConfig {

    @ConfigDescription(
            value = "How long after their last call an account still counts as active, in minutes",
            defaultInt = 15
    )
    int activeUserWindowMinutes();

    @ConfigDescription(
            value = "Maximum number of buckets one activity report may return. Without this cap a"
                    + " hand-written URL is a trivial denial of service against MongoDB.",
            defaultInt = 1000
    )
    int maxBuckets();

    @ConfigDescription(
            value = "Maximum length of a queried period, in days",
            defaultInt = 731
    )
    int maxRangeDays();
}

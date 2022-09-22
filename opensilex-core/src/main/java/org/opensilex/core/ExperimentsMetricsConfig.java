//******************************************************************************
//                          ExperimentsMetricsConfig.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2022
// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core;

import org.opensilex.config.ConfigDescription;
import org.opensilex.service.ServiceConfig;

/**
 * Metrics configuration interface
 *
 * @author Arnaud Charleroy
 */
public interface ExperimentsMetricsConfig extends ServiceConfig {
    @ConfigDescription(
            value = "First metrics for any time depending on is time unit",
            defaultInt = 1
    )
    int timeBeforeFirstMetric();

    @ConfigDescription(
            value = "Delay between whole system metrics (combined with corresponding TimeUnit)",
            defaultInt = 7
    )
    int delayBetweenMetrics();

    @ConfigDescription(
            value = "Default metrics units : DAYS, HOURS, MINUTES, SECONDS are authorized",
            defaultString = "DAYS"
    )
    String metricsTimeUnit(); 
}

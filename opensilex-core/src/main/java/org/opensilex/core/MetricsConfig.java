//******************************************************************************
//                          MetricsConfig.java
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
public interface MetricsConfig extends ServiceConfig {

    @ConfigDescription(
            value = "Activate access metrics",
            defaultBoolean = false
    )
    boolean enableMetrics();

    @ConfigDescription(
            value = "Metrics configs about experiments"
    )
    ExperimentsMetricsConfig experiments();

    @ConfigDescription(
            value = "Metrics configs about system"
    )
    SystemMetricsConfig system();
}

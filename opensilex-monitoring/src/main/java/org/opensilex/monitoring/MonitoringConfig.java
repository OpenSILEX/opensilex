//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.monitoring;

import org.opensilex.config.ConfigDescription;
import org.opensilex.monitoring.config.ActivityConfig;
import org.opensilex.monitoring.config.HealthConfig;
import org.opensilex.monitoring.config.RequestLogConfig;

/**
 * Configuration of the monitoring module, read from the {@code monitoring} block.
 *
 * @author Arnaud Charleroy
 */
public interface MonitoringConfig {

    @ConfigDescription(
            value = "Enable the monitoring module: health check, access log and activity report",
            defaultBoolean = true
    )
    boolean enabled();

    @ConfigDescription(value = "Health check and database statistics options")
    HealthConfig health();

    @ConfigDescription(value = "Web service access log options")
    RequestLogConfig requestLog();

    @ConfigDescription(value = "Activity report options")
    ActivityConfig activity();
}

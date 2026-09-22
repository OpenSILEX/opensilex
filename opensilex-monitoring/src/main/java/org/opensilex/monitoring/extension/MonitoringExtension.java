//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.monitoring.extension;

import java.util.List;
import org.opensilex.OpenSilexExtension;

/**
 * Lets any module contribute its own health probes.
 *
 * <p>Discovered through {@code OpenSilex.getModulesImplementingInterface}, the same mechanism
 * {@code SPARQLExtension} and {@code JCSApiCacheExtension} use. The interface lives here rather
 * than in {@code opensilex-main} so that adding monitoring changes no existing module: a module
 * that wants to publish a probe depends on {@code opensilex-monitoring}, and every other module
 * simply contributes nothing.</p>
 *
 * @author Arnaud Charleroy
 */
public interface MonitoringExtension extends OpenSilexExtension {

    /**
     * @return probes this module contributes, empty by default
     */
    default List<HealthIndicator> getHealthIndicators() {
        return List.of();
    }
}

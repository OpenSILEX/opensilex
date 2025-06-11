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
 * Agroportal API configuration interface
 *
 * @author brice
 */
public interface AgroportalAPIConfig extends ServiceConfig {

    @ConfigDescription(
            value = "Agroportal server path",
            defaultString = "https://agroportal.lirmm.fr"
    )
    String basePath();

    @ConfigDescription(
            value = "Agroportal API path",
            defaultString = "https://data.agroportal.lirmm.fr"
    )
    String baseAPIPath();

    @ConfigDescription(
            value = "Agroportal API key",
            defaultString = ""
    )
    String externalAPIKey();
}

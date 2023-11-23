/*******************************************************************************
 *                         MatomoConfig.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2023.
 * Last Modification: 13/11/2023
 * Contact: valentin.rigolle@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/
package org.opensilex.front.config;

import org.opensilex.config.ConfigDescription;

import java.net.URI;

/**
 * Configuration for Matomo.
 *
 * @author Valentin Rigolle
 */
public interface MatomoConfig {
    @ConfigDescription(
            value = "Matomo server URL"
    )
    String serverUrl();

    @ConfigDescription(
            value = "Site ID of the OpenSilex instance in Matomo"
    )
    Integer siteId();
}

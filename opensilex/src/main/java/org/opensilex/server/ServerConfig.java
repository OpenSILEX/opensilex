//******************************************************************************
//                        ServerConfig.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.server;

import org.opensilex.config.ConfigDescription;
import org.opensilex.module.ModuleConfig;

/**
 * Default configuration for OpenSilex base module
 *
 * @author Vincent Migot
 */
public interface ServerConfig extends ModuleConfig {

    @ConfigDescription(
        value = "Server public URI",
        defaultString = "http://localhost:8666/"
    )
    public String publicURI();
}

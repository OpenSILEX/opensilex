//******************************************************************************
//                          ServerModule.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.server;

import org.opensilex.module.ModuleConfig;
import org.opensilex.OpenSilexModule;

/**
 * <pre>
 * Base module implementation for OpenSilex.
 * - Enable Swagger
 * - Enable Security web services
 * - Enable SPARQL service through configuration
 * - Enable Big Data service through configuration
 * - Enable File System service through configuration
 * - Enable Authentication service through configuration
 * </pre>
 *
 * @see org.opensilex.module.base.ServerConfig
 * @author Vincent Migot
 */
public class ServerModule extends OpenSilexModule {

    @Override
    public Class<? extends ModuleConfig> getConfigClass() {
        return ServerConfig.class;
    }

    @Override
    public String getConfigId() {
        return "server";
    }

}

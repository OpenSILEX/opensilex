//******************************************************************************
//                          BaseModule.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.module.base;

import org.opensilex.module.extensions.APIExtension;
import java.util.*;
import org.opensilex.module.ModuleConfig;
import org.opensilex.module.OpenSilexModule;

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
 * @see org.opensilex.module.base.BaseConfig
 * @author Vincent Migot
 */
public class BaseModule extends OpenSilexModule implements APIExtension {

    @Override
    public Class<? extends ModuleConfig> getConfigClass() {
        return BaseConfig.class;
    }

    @Override
    public String getConfigId() {
        return "opensilex";
    }

    @Override
    public List<String> getPackagesToScan() {
        List<String> list = APIExtension.super.getPackagesToScan();
        list.add("io.swagger.jaxrs.listing");
        list.add("org.opensilex.server.rest");
        list.add("org.opensilex.server.security");

        return list;
    }

}

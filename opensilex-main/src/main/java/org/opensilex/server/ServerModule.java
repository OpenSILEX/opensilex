//******************************************************************************
//                          ServerModule.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.server;

import java.util.List;
import org.opensilex.OpenSilexModule;
import org.opensilex.server.extensions.APIExtension;

/**
 * Tomcat Server integration module for OpenSilex.
 *
 * @see org.opensilex.server.ServerConfig
 * @author Vincent Migot
 */
public class ServerModule extends OpenSilexModule implements APIExtension {

    @Override
    public Class<?> getConfigClass() {
        return ServerConfig.class;
    }

    @Override
    public String getConfigId() {
        return "server";
    }

    @Override
    public List<String> getPackagesToScan() {
        List<String> list = APIExtension.super.getPackagesToScan();
        list.add("org.opensilex.server.rest.serialization");
        list.add("org.opensilex.server.rest.validation");

        return list;
    }

}

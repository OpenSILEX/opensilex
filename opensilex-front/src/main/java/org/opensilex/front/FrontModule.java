/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.front;

import org.opensilex.module.ModuleConfig;
import org.opensilex.module.OpenSilexModule;
import org.opensilex.module.extensions.APIExtension;
import org.opensilex.server.Server;
import org.opensilex.module.extensions.ServerExtension;

/**
 *
 * @author vincent
 */
public class FrontModule extends OpenSilexModule implements ServerExtension, APIExtension, FrontExtension {

    @Override
    public Class<? extends ModuleConfig> getConfigClass() {
        return FrontConfig.class;
    }

    @Override
    public String getConfigId() {
        return "opensilex-front";
    }
    
    @Override
    public void initServer(Server server) {
        server.initApp("/app", "/", "/front", FrontModule.class);
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.front.app;

import org.opensilex.module.OpenSilexModule;
import org.opensilex.server.Server;
import org.opensilex.server.ServerExtension;

/**
 *
 * @author vincent
 */
public class FrontAppModule extends OpenSilexModule implements ServerExtension {

    @Override
    public void initServer(Server server) {
        server.initApp("/app", "/", "/angular/app", FrontAppModule.class);
    }
}

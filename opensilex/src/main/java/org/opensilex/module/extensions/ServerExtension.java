/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.module.extensions;

import org.opensilex.server.Server;

/**
 *
 * @author vincent
 */
public interface ServerExtension {
    
    public void initServer(Server server);
}

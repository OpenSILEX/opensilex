//******************************************************************************
//                      ServerExtension.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.module.extensions;

import org.opensilex.server.Server;

/**
 * Extension interface for OpenSilex modules which add logic at server
 * initialisation and shutdown.
 *
 * @author Vincent Migot
 */
public interface ServerExtension {

    /**
     * Hook on server initialisation
     *
     * @param server Unstarted server instance
     * @throws Exception Can throw anything
     */
    public void initServer(Server server) throws Exception;

    /**
     * Hook on server shutdown
     * 
     * @param server Unstopped server instance
     * @throws Exception Can throw anything
     */
    public void shutDownServer(Server server) throws Exception;
}

//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.module.extensions;

import org.opensilex.server.Server;

/**
 *
 * @author Vincent Migot
 */
public interface ServerExtension {
    
    public void initServer(Server server);
}

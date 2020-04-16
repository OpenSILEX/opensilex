//******************************************************************************
//                           OpenSilexCommand.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.cli;

import org.opensilex.OpenSilex;

/**
 * This interface is requested for any command in OpenSilex modules to be detected on loading.
 *
 * @author Vincent Migot
 */
public interface OpenSilexCommand {

    /**
     * Define application instance.
     *
     * @param opensilex
     */
    public void setOpenSilex(OpenSilex opensilex);

    /**
     * Get application instance.
     *
     * @return application instance
     */
    public OpenSilex getOpenSilex();
}

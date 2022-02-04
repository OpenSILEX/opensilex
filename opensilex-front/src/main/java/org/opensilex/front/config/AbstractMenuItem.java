/*******************************************************************************
 *                         AbstractMenuItem.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2022.
 * Last Modification: 12/01/2022
 * Contact: valentin.rigolle@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/
package org.opensilex.front.config;

import org.opensilex.config.ConfigDescription;

/**
 * @author Valentin RIGOLLE
 */
public interface AbstractMenuItem {

    @ConfigDescription("Menu entry identifier")
    String id();

    @ConfigDescription("Menu entry route (optional)")
    Route route();
}

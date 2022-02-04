/*******************************************************************************
 *                         MenuItemUtils.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2022.
 * Last Modification: 12/01/2022
 * Contact: valentin.rigolle@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/
package org.opensilex.front.config;

import java.util.Collection;

/**
 * @author Valentin RIGOLLE
 */
public class MenuItemUtils {
    public static boolean hasUserCredentials(AbstractMenuItem menuItem, Collection<String> userCredentials) {
        return userCredentials == null // null credentials means the user is admin
                || menuItem.route().credentials() == null || userCredentials.containsAll(menuItem.route().credentials());
    }
}

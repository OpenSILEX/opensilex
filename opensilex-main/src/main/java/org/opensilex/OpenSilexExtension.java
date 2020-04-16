//******************************************************************************
//                            OpenSilexExtension.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex;

/**
 * Default opensilex extension interface.
 *
 * @author Vincent Migot
 */
public interface OpenSilexExtension {

    /**
     * Return OpenSilex instance.
     *
     * @return current opensilex instance
     */
    public OpenSilex getOpenSilex();
}

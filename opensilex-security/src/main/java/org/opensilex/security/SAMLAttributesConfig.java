/*******************************************************************************
 *                         SAMLAttributesConfig.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2022.
 * Last Modification: 07/09/2022
 * Contact: valentin.rigolle@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/
package org.opensilex.security;

import org.opensilex.config.ConfigDescription;

/**
 * Configuration description for the SAML attribute map (related to the
 * Shibboleth attribute-map.xml file)
 *
 * @author Valentin RIGOLLE
 */
public interface SAMLAttributesConfig {
    @ConfigDescription(
            value = "SAML email attribute"
    )
    String email();

    @ConfigDescription(
            value = "SAML first name attribute"
    )
    String firstName();

    @ConfigDescription(
            value = "SAML last name attribute"
    )
    String lastName();
}

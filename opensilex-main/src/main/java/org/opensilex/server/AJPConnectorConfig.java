/*******************************************************************************
 *                         AJPConnectorConfig.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2022.
 * Last Modification: 07/09/2022
 * Contact: valentin.rigolle@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/
package org.opensilex.server;

import org.opensilex.config.ConfigDescription;

/**
 * Configuration for the AJP connector for communication between Apache and Tomcat. Used to implement the
 * Shibboleth / SAML authentication.
 *
 * @author Valentin RIGOLLE
 */
public interface AJPConnectorConfig {
    @ConfigDescription(
            value = "Enable the AJP connector",
            defaultBoolean = false
    )
    boolean enable();

    @ConfigDescription(
            value = "The connector port where Tomcat will listen to AJP messages",
            defaultInt = 8009
    )
    int port();

    @ConfigDescription(
            value = "The secret to use for AJP communication with Apache. If empty, no secret will be used."
    )
    String secret();
}

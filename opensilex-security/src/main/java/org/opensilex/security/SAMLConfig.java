/*******************************************************************************
 *                         SAMLConfig.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2022.
 * Last Modification: 07/09/2022
 * Contact: valentin.rigolle@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/
package org.opensilex.security;

import org.opensilex.config.ConfigDescription;

import java.util.Map;

/**
 * Configuration description for SAML authentication (using Shibboleth)
 *
 * @author Valentin RIGOLLE
 */
public interface SAMLConfig {
    @ConfigDescription(
            value = "Enable SAML",
            defaultBoolean = false
    )
    boolean enable();

    @ConfigDescription(
            value = "The SAML proxy URI protected by Shibboleth to initiate SSO login",
            defaultString = "http://localhost/opensilex-saml"
    )
    String samlProxyLoginURI();

    @ConfigDescription(
            value = "SAML landing page where the user is redirected to after logging in",
            defaultString = "http://localhost:8666/app/saml"
    )
    String samlLandingPageURI();

    @ConfigDescription(
            value = "SAML attributes names as defined in Shibboleth file attribute-map.xml"
    )
    SAMLAttributesConfig attributes();

    @ConfigDescription(
            value = "SAML connection link title",
            defaultMap = {
                    "en: Log in with SSO (SAML)",
                    "fr: Connexion par SSO (SAML)"
            }
    )
    Map<String, String> connectionTitle();
}

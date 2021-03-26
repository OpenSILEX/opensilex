//******************************************************************************
//                        SecurityConfig.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.security;

import java.util.Map;
import org.opensilex.config.ConfigDescription;

public interface OpenIDConfig {

    @ConfigDescription(
            value = "Enable OpenID",
            defaultBoolean = false
    )
    boolean enable();

    @ConfigDescription(
            value = "OpenID Connect provider URI",
            defaultString = "https://aai-dev.egi.eu/oidc/"
    )
    String providerURI();

    @ConfigDescription(
            value = "OpenID Connect redirect URI",
            defaultString = "http://localhost:8080/app/openid"
    )
    String redirectURI();

    @ConfigDescription(
            value = "OpenID Connect client identifier",
            defaultString = "574f1d35-58ac-4e43-9515-f4c062ec29b7"
    )
    String clientID();

    @ConfigDescription(
            value = "OpenID Connect client secret",
            defaultString = "AKIChvkBeMUUy95DzB_pYyv2ejTmPfX7omiyc9x0bsutws09rYkE-tclIefCiW05qNnuW3t9vmGSp0zoy9El1q0"
    )
    String clientSecret();

    @ConfigDescription(
            value = "OpenID connection link title",
            defaultMap = {"en: Connect with EGI Check-in", "fr: Connexion avec EGI Check-in"}
    )
    Map<String, String> connectionTitle();
}

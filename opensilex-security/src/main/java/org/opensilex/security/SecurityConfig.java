//******************************************************************************
//                        SecurityConfig.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.security;

import org.opensilex.config.ConfigDescription;
import org.opensilex.security.authentication.AuthenticationService;

/**
 * Default configuration for OpenSilex base module
 *
 * @author Vincent Migot
 */
public interface SecurityConfig {

    @ConfigDescription(
            value = "Authentication service"
    )
    public AuthenticationService authentication();

    @ConfigDescription(
            value = "Option to allow multiple connection with the same account (NOT RECOMMENDED IN PRODUCTION)",
            defaultBoolean = false
    )
    public boolean allowMultiConnection();

    @ConfigDescription(
            value = "OpenID configuration (disable by default)"
    )
    public OpenIDConfig openID();
}

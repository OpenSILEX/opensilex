//******************************************************************************
//                        SecurityConfig.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.security;

import org.opensilex.config.ConfigDescription;
import org.opensilex.security.authentication.AuthenticationService;
import org.opensilex.security.email.EmailService;
 
/**
 * Default configuration for OpenSilex base module
 *
 * @author Vincent Migot
 */
public interface SecurityConfig {

    @ConfigDescription(
            value = "Authentication service"
    )
    AuthenticationService authentication();

    
     @ConfigDescription(
            value = "Email service"
    )
    EmailService email();
    
    @ConfigDescription(
            value = "Option to allow multiple connection with the same account (NOT RECOMMENDED IN PRODUCTION)",
            defaultBoolean = false
    )
    boolean allowMultiConnection();

    @ConfigDescription(
            value = "OpenID configuration (disable by default)"
    )
    OpenIDConfig openID();

    @ConfigDescription(
            value = "SAML configuration (disable by default)"
    )
    SAMLConfig saml();
}

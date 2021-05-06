//******************************************************************************
//                        EmailConfig.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2021
// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.security;

import org.opensilex.config.ConfigDescription;
import org.opensilex.service.ServiceConfig;

/**
 * Email service configuration
 * @see https://www.simplejavamail.org/#navigation
 * @author Arnaud Charleroy
 */
public interface EmailConfig extends ServiceConfig {
    @ConfigDescription(
        value = "Enable e-mail service",
        defaultBoolean = false
    )
    public Boolean enable();
    
    @ConfigDescription(
        value = "Logging e-mail transport without sending",
        defaultBoolean = false
    )
    public Boolean simulateSending();
    
    @ConfigDescription(
            value = "Email sender (Not working)",
            defaultString = "team@opensilex.org"
    )
    public String sender();
    
    public SMTPConfig smtp();
 
        
}

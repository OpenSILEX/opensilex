//******************************************************************************
//                        SMTPConfig.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2021
// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.security;

import org.opensilex.config.ConfigDescription;

/**
 * SMTP server connexion settings
 * @see https://www.simplejavamail.org/#navigation
 * @author Arnaud Charleroy
 */
public interface SMTPConfig{
    
    @ConfigDescription(
            value = "SMTP host",
            defaultString = "smtp.gmail.com"
    )
    public String host();

    @ConfigDescription(
            value = "SMTP port",
            defaultInt = 587
    )
    public int port(); 

    @ConfigDescription(
            value = "SMTP account id",
            defaultString =""
    )
    public String userId();

    @ConfigDescription(
            value = "SMTP account password",
             defaultString =""
    )
    public String userPassword(); 

}

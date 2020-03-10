//******************************************************************************
//                        RestConfig.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.rest;

import java.util.List;
import org.opensilex.OpenSilex;
import org.opensilex.config.ConfigDescription;
import org.opensilex.module.ModuleConfig;
import org.opensilex.rest.authentication.AuthenticationService;

/**
 * Default configuration for OpenSilex base module
 *
 * @author Vincent Migot
 */
public interface RestConfig extends ModuleConfig {

    @ConfigDescription(
            value = "Flag to determine if application is in debug mode or not",
            defaultBoolean = false
    )
    public Boolean debug();

    @ConfigDescription(
            value = "Default application language",
            defaultString = OpenSilex.DEFAULT_LANGUAGE
    )
    public String defaultLanguage();
    
        @ConfigDescription(
            value = "Available application language list",
            defaultList = {OpenSilex.DEFAULT_LANGUAGE, "fr-FR"}
    )
    public List<String> availableLanguages();

    @ConfigDescription(
            value = "Authentication service"
    )
    public AuthenticationService authentication();

}

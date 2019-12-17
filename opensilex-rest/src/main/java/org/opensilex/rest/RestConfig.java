//******************************************************************************
//                        RestConfig.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.rest;

import org.opensilex.bigdata.BigDataService;
import org.opensilex.config.ConfigDescription;
import org.opensilex.module.ModuleConfig;
import org.opensilex.fs.FileStorageService;
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
            defaultString = "en"
    )
    public String defaultLanguage();

    @ConfigDescription(
            value = "Default file storage directory"
    )
    public String storageBasePath();

    @ConfigDescription(
            value = "Big data source"
    )
    public BigDataService bigData();

    @ConfigDescription(
            value = "File storage service"
    )
    public FileStorageService fs();

    @ConfigDescription(
            value = "Authentication service"
    )
    public AuthenticationService authentication();

}

//******************************************************************************
//                        ServerConfig.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.server;

import java.util.List;
import java.util.Map;
import org.opensilex.OpenSilex;
import org.opensilex.config.ConfigDescription;

/**
 * Default configuration for OpenSilex base module
 *
 * @author Vincent Migot
 */
public interface ServerConfig {

    @ConfigDescription(
            value = "Server public URI",
            defaultString = "http://localhost:8666/"
    )
    public String publicURI();

    @ConfigDescription(
            value = "Default application language",
            defaultString = OpenSilex.DEFAULT_LANGUAGE
    )
    public String defaultLanguage();

    @ConfigDescription(
            value = "Available application language list",
            defaultList = {OpenSilex.DEFAULT_LANGUAGE, "fr"}
    )
    public List<String> availableLanguages();

    @ConfigDescription(
            value = "Modules loading order list"
    )
    public List<String> modulesOrder();

    @ConfigDescription(
            value = "Tomcat system properties"
    )
    public Map<String, String> tomcatSystemProperties();

    @ConfigDescription(
            value = "Enable Tomcat anti-thread lock mechanism with StuckThreadDetectionValve",
            defaultBoolean = true
    )
    public boolean enableAntiThreadLock();

}

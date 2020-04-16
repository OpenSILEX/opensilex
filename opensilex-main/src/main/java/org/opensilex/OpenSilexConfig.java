//******************************************************************************
//                            OpenSilexConfig.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex;

import java.util.List;
import java.util.Map;
import org.opensilex.config.ConfigDescription;

/**
 * OpenSilex system configuration description.
 *
 * @author Vincent Migot
 */
public interface OpenSilexConfig {

    /**
     * YAML key for system configuration.
     */
    public final static String YAML_KEY = "system";

    @ConfigDescription(
            value = "Default application language",
            defaultString = OpenSilex.DEFAULT_LANGUAGE
    )
    public String defaultLanguage();

    @ConfigDescription(
            value = "Maven modules loading order list"
    )
    public List<String> modulesOrder();

    @ConfigDescription(
            value = "Modules classes to ignore"
    )
    public Map<String, String> ignoredModules();

}

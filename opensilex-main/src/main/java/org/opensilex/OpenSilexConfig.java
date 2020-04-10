/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex;

import java.util.List;
import java.util.Map;
import org.opensilex.config.ConfigDescription;

/**
 *
 * @author vince
 */
public interface OpenSilexConfig {

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

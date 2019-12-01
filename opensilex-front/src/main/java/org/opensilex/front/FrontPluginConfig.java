/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.front;

import java.nio.file.Path;
import java.util.List;
import org.opensilex.config.ConfigDescription;

/**
 *
 * @author vincent
 */
public interface FrontPluginConfig {

    @ConfigDescription(
            value = "Front plugin path"
    )
    Path path();

    @ConfigDescription(
            value = "Front plugin class"
    )
    String module();

    List<String> dependencies();
    
    List<FrontPluginMenuConfig> menu();

}

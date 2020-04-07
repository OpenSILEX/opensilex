/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.front.theme;

import java.util.List;
import java.util.Map;
import org.opensilex.config.ConfigDescription;

public interface ThemeConfig {

    @ConfigDescription(
            value = "Parent theme identifier to extend"
    )
    String extend();

    @ConfigDescription(
            value = "URI of the favicon"
    )
    String favicon();

    @ConfigDescription(
            value = "List stylesheets files to include (css or scss)"
    )
    List<String> stylesheets();

    @ConfigDescription(
            value = "List of stylesheets to ignore from extended theme"
    )
    List<String> excludes();

    @ConfigDescription(
            value = "List of fonts configuration"
    )
    List<FontConfig> fonts();

    @ConfigDescription(
            value = "Map of icon class by ontology URI"
    )
    Map<String, String> iconClassesRDF();
}

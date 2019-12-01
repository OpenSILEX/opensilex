/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.front;

import org.opensilex.config.ConfigDescription;

/**
 *
 * @author vincent
 */
public interface FrontPluginMenuConfig {

    @ConfigDescription(
        value = "Angular menu entry path"
    )
    String path();

    @ConfigDescription(
        value = "Angular menu entry label"
    )
    String label();
    
}

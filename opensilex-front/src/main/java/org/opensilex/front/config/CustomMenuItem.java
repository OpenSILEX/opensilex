/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.front.config;

import java.util.List;
import java.util.Map;
import org.opensilex.config.ConfigDescription;

/**
 *
 * @author vmigot
 */
public interface CustomMenuItem {
    
    @ConfigDescription("Menu entry identifier")
    public String id();
    
    @ConfigDescription("Menu entry label trnaslations")
    public Map<String, String> label();

    @ConfigDescription("Menu entry children")
    public List<CustomMenuItem> children();

    @ConfigDescription("Menu entry route (optional)")
    public Route route();
}

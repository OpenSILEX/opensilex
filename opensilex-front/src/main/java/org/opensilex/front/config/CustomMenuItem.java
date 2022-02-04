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
public interface CustomMenuItem extends AbstractMenuItem {
    @ConfigDescription("Menu entry label trnaslations")
    Map<String, String> label();

    @ConfigDescription("Menu entry children")
    List<CustomMenuItem> children();
}

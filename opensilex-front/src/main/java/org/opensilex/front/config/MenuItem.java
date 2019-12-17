/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.front.config;

import java.util.List;
import org.opensilex.config.ConfigDescription;

/**
 *
 * @author vidalmor
 */
public interface MenuItem {
    
    @ConfigDescription("Menu entry identifier")
    public String id();
    
    @ConfigDescription("Menu entry label")
    public String label();

    @ConfigDescription("Menu entry children")
    public List<MenuItem> children();

    @ConfigDescription("Menu entry route (optional)")
    public Route route();
}

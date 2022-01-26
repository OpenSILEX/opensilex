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
public interface MenuItem extends AbstractMenuItem {
    
    @ConfigDescription("Menu entry label")
    String label();

    @ConfigDescription("Menu entry children")
    List<MenuItem> children();
}

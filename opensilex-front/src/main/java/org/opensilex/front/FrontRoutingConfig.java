/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.front;

import java.util.List;
import org.opensilex.config.ConfigDescription;

/**
 *
 * @author vidalmor
 */
public interface FrontRoutingConfig  {
    
    @ConfigDescription("Menu entries")
    public List<MenuItem> menu();
    
    @ConfigDescription("Other front routes")
    public List<Route> routes();
}

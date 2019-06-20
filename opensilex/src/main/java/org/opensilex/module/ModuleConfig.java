/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.module;

import java.util.Map;
import org.opensilex.service.ServiceConfig;

/**
 *
 * @author vincent
 */
public interface ModuleConfig {
    
    Map<String, ServiceConfig> services();
}

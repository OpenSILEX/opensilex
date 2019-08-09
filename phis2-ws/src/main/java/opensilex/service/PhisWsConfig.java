/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package opensilex.service;

import org.opensilex.module.ModuleConfig;

/**
 *
 * @author vincent
 */
public interface PhisWsConfig extends ModuleConfig {
    
    PhisPostgreSQLConfig postgreSQL();
    
    PhisServiceConfig service();
}

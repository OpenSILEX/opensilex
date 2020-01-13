/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.nosql;

import org.opensilex.nosql.service.NoSQLService;
import org.opensilex.config.ConfigDescription;
import org.opensilex.module.ModuleConfig;

/**
 *
 * @author vidalmor
 */
public interface NoSQLConfig extends ModuleConfig {

    @ConfigDescription(
            value = "No SQL data source"
    )
    public NoSQLService nosql();
    
}

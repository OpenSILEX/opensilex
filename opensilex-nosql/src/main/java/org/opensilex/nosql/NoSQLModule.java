/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.nosql;

import org.opensilex.OpenSilexModule;
import org.opensilex.module.ModuleConfig;

/**
 *
 * @author vidalmor
 */
public class NoSQLModule extends OpenSilexModule {

    @Override
    public Class<? extends ModuleConfig> getConfigClass() {
        return NoSQLConfig.class;
    }

    @Override
    public String getConfigId() {
        return "big-data";
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.module.base;

import java.util.List;
import org.opensilex.module.ModuleConfig;
import org.opensilex.module.OpenSilexModule;

/**
 *
 * @author vincent
 */
public class BaseModule extends OpenSilexModule {

    @Override
    public Class<? extends ModuleConfig> getConfigClass() {
        return BaseConfig.class;
    }

    @Override
    public String getConfigId() {
        return "opensilex";
    }

    @Override
    public List<String> getPackagesToScan() {
        List<String> list = super.getPackagesToScan();
        list.add("io.swagger.jaxrs.listing");
        list.add("org.opensilex.server.rest");
        list.add("org.opensilex.server.security");

        return list;
    }

}

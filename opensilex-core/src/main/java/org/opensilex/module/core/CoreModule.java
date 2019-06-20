//******************************************************************************
//                             CoreModule.java
// OpenSILEX
// Copyright © INRA 2019
// Creation date: 01 jan. 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.module.core;

import org.opensilex.module.Module;
import java.util.List;
import org.opensilex.module.ModuleConfig;

/**
 * Core OpenSILEX module implementation
 */
public class CoreModule extends Module {

    protected String configId = "opensilex-core";
    protected Class<? extends ModuleConfig> configClass =  CoreConfig.class;
        
    @Override
    public List<String> getPackagesToScan() {
        List<String> list = super.getPackagesToScan();
        list.add("io.swagger.jaxrs.listing");
        
        return list;
    }
}

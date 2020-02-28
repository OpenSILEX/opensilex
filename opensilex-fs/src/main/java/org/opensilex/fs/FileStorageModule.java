/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.fs;

import java.nio.file.Paths;
import org.opensilex.OpenSilex;
import org.opensilex.OpenSilexModule;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.module.ModuleConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author vidalmor
 */
public class FileStorageModule extends OpenSilexModule {

    private final static Logger LOGGER = LoggerFactory.getLogger(FileStorageModule.class);
    
    @Override
    public Class<? extends ModuleConfig> getConfigClass() {
        return FileStorageConfig.class;
    }

    @Override
    public String getConfigId() {
        return "file-system";
    }

    @Override
    public void startup() throws Exception {
        FileStorageService fs = OpenSilex.getInstance().getServiceInstance("fs", FileStorageService.class);
        String basePath = ((FileStorageConfig) getConfig()).storageBasePath();
        LOGGER.debug("Setup base storage path: " + basePath);
        fs.setStorageBasePath(Paths.get(basePath));
    }

}

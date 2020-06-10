/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.fs;

import org.opensilex.OpenSilexModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author vidalmor
 */
public class FileStorageModule extends OpenSilexModule {

    private final static Logger LOGGER = LoggerFactory.getLogger(FileStorageModule.class);

    @Override
    public Class<?> getConfigClass() {
        return FileStorageConfig.class;
    }

    @Override
    public String getConfigId() {
        return "file-system";
    }

}

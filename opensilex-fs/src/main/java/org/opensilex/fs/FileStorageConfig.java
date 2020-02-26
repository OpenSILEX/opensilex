/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.fs;

import org.opensilex.fs.service.FileStorageService;
import org.opensilex.config.ConfigDescription;
import org.opensilex.fs.local.LocalFileSystemConfig;
import org.opensilex.fs.local.LocalFileSystemConnection;
import org.opensilex.module.ModuleConfig;
import org.opensilex.service.ServiceConfigDefault;

public interface FileStorageConfig extends ModuleConfig {

    @ConfigDescription(
            value = "File storage access"
    )
    public FileStorageService fs();
}

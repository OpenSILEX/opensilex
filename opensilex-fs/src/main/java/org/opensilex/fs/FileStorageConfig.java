/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.fs;

import org.opensilex.fs.service.FileStorageService;
import org.opensilex.config.ConfigDescription;
import org.opensilex.service.ServiceConfig;

public interface FileStorageConfig extends ServiceConfig {

    @ConfigDescription(
            value = "File storage service"
    )
    public FileStorageService fs();

    @ConfigDescription(
            value = "File storage service",
            defaultInt = 600
    )
    int readTimeoutSeconds();

}

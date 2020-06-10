/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.fs;

import org.opensilex.fs.service.FileStorageService;
import org.opensilex.config.ConfigDescription;

public interface FileStorageConfig {

    @ConfigDescription(
            value = "File storage service"
    )
    public FileStorageService fs();

}

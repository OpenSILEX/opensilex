/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.fs.service;

import java.util.Map;
import org.opensilex.config.ConfigDescription;
import org.opensilex.service.ServiceConfig;

/**
 *
 * @author vince
 */
public interface FileStorageServiceConfig extends ServiceConfig { 
    
    @ConfigDescription(
        value = "Default file system storage"
    )
    public String defaultFS();

    @ConfigDescription(
            value = "Map of file storage connection definition by identifier"
     )
    public Map<String, FileStorageConnection> connections();

    @ConfigDescription(
            value = "Map of custom path connection management"
    )
    public Map<String, String> customPath();

}

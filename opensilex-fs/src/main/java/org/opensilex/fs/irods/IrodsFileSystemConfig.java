/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.fs.irods;

import org.opensilex.config.ConfigDescription;
import org.opensilex.service.ServiceConfig;

/**
 *
 * @author Alice Boizet
 */
public interface IrodsFileSystemConfig extends ServiceConfig {
    
    @ConfigDescription(
            value = "Base path for file storage"
    )
    public String basePath();
    
}

//******************************************************************************
//                          MongoDBService.java
// OpenSILEX
// Copyright © INRA 2019
// Creation date: 01 jan. 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.module.core.service.fs;

import org.opensilex.module.core.service.fs.local.LocalFileSystemConfig;


public class FileStorageService {
    /**
     * FileSystem connection configuration
     */
    private LocalFileSystemConfig config;
    
    /**
     * Constructor for FileSystem service with configuration
     * 
     * @param config FileSystem connection configuration
     */
    public FileStorageService(LocalFileSystemConfig config) {
        this.config = config;
    }
}

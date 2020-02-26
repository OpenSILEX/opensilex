//******************************************************************************
//                         FileStorageService.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.fs.service;

import java.io.File;
import java.nio.file.Path;
import org.opensilex.fs.local.LocalFileSystemConfig;
import org.opensilex.fs.local.LocalFileSystemConnection;
import org.opensilex.service.Service;
import org.opensilex.service.ServiceConfigDefault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * File storage service to access any filesystem (default to local)
 *
 * @author Vincent Migot
 */
@ServiceConfigDefault(
        connection = LocalFileSystemConnection.class,
        connectionConfig = LocalFileSystemConfig.class,
        connectionConfigID = "local"
)
public class FileStorageService implements Service, FileStorageConnection {

    private final static Logger LOGGER = LoggerFactory.getLogger(FileStorageService.class);

    @Override
    public void readFile(Path filePath) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void writeFile(Path filePath, String content) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void createDirectories(Path directoryPath) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void writeFile(Path filePath, File file) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }


}

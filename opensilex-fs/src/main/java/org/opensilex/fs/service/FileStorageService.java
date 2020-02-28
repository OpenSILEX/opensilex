//******************************************************************************
//                         FileStorageService.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.fs.service;

import java.io.File;
import java.nio.file.Path;
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
        connection = LocalFileSystemConnection.class
)
public class FileStorageService implements Service, FileStorageConnection {

    private final static Logger LOGGER = LoggerFactory.getLogger(FileStorageService.class);

    private final FileStorageConnection connection;

    public FileStorageService(FileStorageConnection connection) {
        this.connection = connection;
    }
    
    private Path storageBasePath;

    public Path getStorageBasePath() {
        return storageBasePath;
    }

    public void setStorageBasePath(Path storageBasePath) {
        this.storageBasePath = storageBasePath;
    }

    @Override
    public String readFile(Path filePath) throws Exception {
        Path absolutePath = storageBasePath.resolve(filePath).toAbsolutePath();
        LOGGER.debug("READ FILE: " + absolutePath.toString());
        return this.connection.readFile(absolutePath);
    }

    @Override
    public void writeFile(Path filePath, String content) throws Exception {
        Path absolutePath = storageBasePath.resolve(filePath).toAbsolutePath();
        LOGGER.debug("WRITE FILE: " + absolutePath.toString());
        this.connection.writeFile(absolutePath, content);
    }

    
    @Override
    public void writeFile(Path filePath, File file) throws Exception {
        Path absolutePath = storageBasePath.resolve(filePath).toAbsolutePath();
        LOGGER.debug("WRITE FILE: " + absolutePath.toString());
        this.connection.writeFile(absolutePath, file);
    }
    
    @Override
    public void createDirectories(Path directoryPath) throws Exception {
        Path absolutePath = storageBasePath.resolve(directoryPath).toAbsolutePath();
         LOGGER.debug("CREATE DIRECTORIES: " + absolutePath.toString());
        this.connection.createDirectories(absolutePath);
    }
}

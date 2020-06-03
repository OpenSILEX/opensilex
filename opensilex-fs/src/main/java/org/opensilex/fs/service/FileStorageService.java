//******************************************************************************
//                         FileStorageService.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.fs.service;

import org.opensilex.fs.local.LocalFileSystemConnection;
import org.opensilex.service.BaseService;
import org.opensilex.service.Service;
import org.opensilex.service.ServiceDefaultDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * File storage service to access any filesystem (default to local)
 *
 * @author Vincent Migot
 */
@ServiceDefaultDefinition(
        serviceClass = LocalFileSystemConnection.class
)
public class FileStorageService extends BaseService implements Service, FileStorageConnection {

    private final static Logger LOGGER = LoggerFactory.getLogger(FileStorageService.class);

    public final static String DEFAULT_FS_SERVICE = "fs";

    private final FileStorageConnection connection;

    public FileStorageService(FileStorageConnection connection) {
        this.connection = connection;
    }

    @Override
    public void setup() throws Exception {
        connection.setup();
    }

    @Override
    public void shutdown() throws Exception {
        connection.shutdown();
    }

    @Override
    public void clean() throws Exception {
        connection.clean();
    }

    @Override
    public void startup() throws Exception {
        connection.startup();
    }

    private Path storageBasePath;

    public Path getStorageBasePath() {
        return storageBasePath;
    }

    public void setStorageBasePath(Path storageBasePath) {
        this.storageBasePath = storageBasePath;
    }

    public Path getAbsolutePath(Path filePath) {
        return filePath.isAbsolute() ? filePath : storageBasePath.resolve(filePath).toAbsolutePath();
    }

    @Override
    public String readFile(Path filePath) throws IOException {
        Path absolutePath = getAbsolutePath(filePath);
        LOGGER.debug("READ FILE: " + absolutePath.toString());
        return connection.readFile(absolutePath);
    }

    @Override
    public byte[] readFileAsByteArray(Path filePath) throws IOException {
        Path absolutePath = getAbsolutePath(filePath);
        LOGGER.debug("READ FILE AS BYTE ARRAY: " + absolutePath.toString());
        return connection.readFileAsByteArray(absolutePath);
    }

    @Override
    public Path getPhysicalPath(Path filePath) throws IOException {
        Path absolutePath = getAbsolutePath(filePath);
        LOGGER.debug("FETCH FILE : " + absolutePath.toString());
        return connection.getPhysicalPath(absolutePath);
    }

    @Override
    public void writeFile(Path filePath, String content) throws IOException {
        Path absolutePath = getAbsolutePath(filePath);
        LOGGER.debug("WRITE FILE: " + absolutePath.toString());
        connection.writeFile(absolutePath, content);
    }


    @Override
    public void writeFile(Path filePath, File file) throws IOException {
        Path absolutePath = getAbsolutePath(filePath);
        LOGGER.debug("WRITE FILE: " + absolutePath.toString());
        connection.writeFile(absolutePath, file);
    }

    @Override
    public void createDirectories(Path directoryPath) throws IOException {
        Path absolutePath = getAbsolutePath(directoryPath);
        LOGGER.debug("CREATE DIRECTORIES: " + absolutePath.toString());
        connection.createDirectories(absolutePath);
    }

    @Override
    public Path createFile(Path filePath) throws IOException {
        Path absolutePath = getAbsolutePath(filePath);
        LOGGER.debug("CREATE FILES: " + absolutePath.toString());
        return connection.createFile(filePath);
    }

    @Override
    public boolean exist(Path filePath) throws IOException {
        Path absolutePath = getAbsolutePath(filePath);
        LOGGER.debug("CHECK EXISTS: " + absolutePath.toString());
        return connection.exist(absolutePath);
    }

    @Override
    public void delete(Path filePath) throws IOException {
        Path absolutePath = getAbsolutePath(filePath);
        LOGGER.debug("DELETE: " + absolutePath.toString());
        connection.delete(absolutePath);
    }
}

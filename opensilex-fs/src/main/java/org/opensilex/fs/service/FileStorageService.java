//******************************************************************************
//                         FileStorageService.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.fs.service;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opensilex.config.InvalidConfigException;
import org.opensilex.fs.local.LocalFileSystemConnection;
import org.opensilex.service.BaseService;
import org.opensilex.service.Service;
import org.opensilex.service.ServiceDefaultDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;

/**
 * File storage service to access any filesystem (default to local)
 *
 * @author Vincent Migot
 */
@ServiceDefaultDefinition(config = FileStorageServiceConfig.class)
public class FileStorageService extends BaseService implements Service, FileStorageConnection {

    private final static Logger LOGGER = LoggerFactory.getLogger(FileStorageService.class);

    public final static String DEFAULT_FS_SERVICE = "fs";

    private final FileStorageConnection local;

    private final Map<Path, FileStorageConnection> customPath = new HashMap<>();

    private final List<Path> pathOrder;

    public FileStorageService(FileStorageServiceConfig config) throws InvalidConfigException {
        super(config);
        this.local = new LocalFileSystemConnection(Paths.get(config.basePath()));

        Map<String, FileStorageConnection> connections = config.connections();
        for (Map.Entry<String, String> path : config.customPath().entrySet()) {
            Path pathPrefix = Paths.get(path.getKey());
            String connectionID = path.getValue();

            if (!connections.containsKey(connectionID)) {
                throw new InvalidConfigException("File storage connection not found: " + connectionID);
            }

            customPath.put(pathPrefix, connections.get(connectionID));
        }

        pathOrder = new ArrayList<>(customPath.keySet());

        pathOrder.sort((Path p1, Path p2) -> {
            if (p1 == null) {
                if (p2 == null) {
                    return 0;
                } else {
                    return -1;
                }
            } else {
                if (p2 == null) {
                    return 1;
                } else if (p1.equals(p2)) {
                    return 0;
                } else if (p1.startsWith(p2)) {
                    return 1;
                } else if (p2.startsWith(p1)) {
                    return -1;
                } else {
                    return p1.toAbsolutePath().toString().compareTo(p2.toAbsolutePath().toString());
                }
            }
        });
    }

    public FileStorageServiceConfig getImplementedConfig() {
        return (FileStorageServiceConfig) this.getConfig();
    }

    public Path getStorageBasePath() {
        return Paths.get(getImplementedConfig().basePath());
    }

    protected FileStorageConnection getConnection(Path filePath) {
        for (Path candidatePath : pathOrder) {
            if (filePath.startsWith(candidatePath)) {
                return customPath.get(candidatePath);
            }
        }

        return this.local;
    }

    @Override
    public String readFile(Path filePath) throws Exception {
        LOGGER.debug("READ FILE: " + filePath.toString());
        return getConnection(filePath).readFile(filePath);
    }

    @Override
    public void writeFile(Path filePath, String content) throws Exception {
        LOGGER.debug("WRITE FILE: " + filePath.toString());
        getConnection(filePath).writeFile(filePath, content);
    }

    @Override
    public void writeFile(Path filePath, File file) throws Exception {
        LOGGER.debug("WRITE FILE: " + filePath.toString());
        getConnection(filePath).writeFile(filePath, file);
    }

    @Override
    public void createDirectories(Path directoryPath) throws Exception {
        LOGGER.debug("CREATE DIRECTORIES: " + directoryPath.toString());
        getConnection(directoryPath).createDirectories(directoryPath);
    }

    @Override
    public byte[] readFileAsByteArray(Path filePath) throws Exception {
        LOGGER.debug("READ FILE BYTES: " + filePath.toString());
        return getConnection(filePath).readFileAsByteArray(filePath);
    }

    @Override
    public Path createFile(Path filePath) throws Exception {
        LOGGER.debug("CREATE FILE: " + filePath.toString());
        return getConnection(filePath).createFile(filePath);
    }

    @Override
    public boolean exist(Path filePath) throws Exception {
        LOGGER.debug("TEST FILE EXISTENCE: " + filePath.toString());
        return getConnection(filePath).exist(filePath);
    }

    @Override
    public void delete(Path filePath) throws Exception {
        LOGGER.debug("DELETE FILE: " + filePath.toString());
        getConnection(filePath).delete(filePath);
    }
}

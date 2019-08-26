//******************************************************************************
//                          MongoDBService.java
// OpenSILEX
// Copyright © INRA 2019
// Creation date: 01 jan. 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.fs;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;
import org.opensilex.fs.local.LocalFileSystemConfig;
import org.opensilex.service.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileStorageService implements Service {

    private final static Logger LOGGER = LoggerFactory.getLogger(FileStorageService.class);

    /**
     * FileSystem connection configuration
     */
    private LocalFileSystemConfig config;

    /**
     * Constructor for FileSystem service with configuration
     *
     * @param config FileSystem connection configuration
     */
    public FileStorageService() {
//        this.config = config;
    }

    public void listFilesByExtension(String directory, String extensionFilter, Consumer<File> action) throws IOException {
        Path directoryPath = Paths.get(directory);

        LOGGER.debug("Load files by extension: " + directory + " " + extensionFilter);

        Files.walk(directoryPath)
                .filter(Files::isRegularFile)
                .map(p -> p.toFile())
                .filter(f -> f.getAbsolutePath().endsWith("." + extensionFilter))
                .forEach(action);
    }
}

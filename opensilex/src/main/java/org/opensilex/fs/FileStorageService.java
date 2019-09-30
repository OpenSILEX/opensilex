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

    public void listFilesByExtension(String directory, String extensionFilter, Consumer<File> action) throws IOException {
        Path directoryPath = Paths.get(directory);

        LOGGER.debug("Load files by extension: " + directory + " " + extensionFilter);

        if (Files.exists(directoryPath) && Files.isDirectory(directoryPath)) {
            Files.walk(directoryPath)
                    .filter(Files::isRegularFile)
                    .map(p -> p.toFile())
                    .filter(f -> f.getAbsolutePath().endsWith("." + extensionFilter))
                    .forEach(action);
        }
    }
}

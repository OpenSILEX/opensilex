/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.fs.local;

import java.io.IOException;
import java.nio.file.Files;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author vmigot
 */
public class TempFileSystemConnection extends LocalFileSystemConnection {

    private final static Logger LOGGER = LoggerFactory.getLogger(TempFileSystemConnection.class);

    public TempFileSystemConnection() throws IOException {
        super(Files.createTempDirectory("opensilex-temp"));
        
        // Add shutdown hook to delete temporary directory on application stop
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    FileUtils.deleteDirectory(getBasePath().toFile());
                } catch (IOException ex) {
                    LOGGER.error("Error while deleting opensilex temporary directory", ex);
                }
            }
        });
        
    }
}

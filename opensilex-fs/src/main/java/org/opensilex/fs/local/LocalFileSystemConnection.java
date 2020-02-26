//******************************************************************************
//                  LocalFileSystemConnection.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.fs.local;

import java.io.File;
import java.nio.file.Path;
import org.opensilex.fs.service.FileStorageConnection;

/**
 * Local filesystem connection for file storage service
 * <pre>
 * TODO create a real implementation with config
 * </pre>
 *
 * @see org.opensilex.fs.service.FileStorageService
 * @author Vincent Migot
 */
public class LocalFileSystemConnection implements FileStorageConnection {

    private final LocalFileSystemConfig config;

    public LocalFileSystemConnection(LocalFileSystemConfig config) {
        this.config = config;
    }

    @Override
    public void readFile(Path filePath) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void writeFile(Path filePath, String content) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void writeFile(Path filePath, File file) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void createDirectories(Path directoryPath) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}

//******************************************************************************
//                  LocalFileSystemConnection.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.fs.local;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.apache.commons.io.FileUtils;
import org.opensilex.fs.service.FileStorageConnection;

/**
 * Local filesystem connection for file storage service
 *
 * @see org.opensilex.fs.service.FileStorageService
 * @author Vincent Migot
 */
public class LocalFileSystemConnection implements FileStorageConnection {

    @Override
    public String readFile(Path filePath) throws Exception {
        return FileUtils.readFileToString(filePath.toFile(), StandardCharsets.UTF_8);
    }

    @Override
    public void writeFile(Path filePath, String content) throws Exception {
        FileUtils.writeStringToFile(filePath.toFile(), content, StandardCharsets.UTF_8);
    }

    @Override
    public void writeFile(Path filePath, File file) throws Exception {
        FileUtils.copyFile(file, filePath.toFile());
    }

    @Override
    public void createDirectories(Path directoryPath) throws Exception {
        FileUtils.forceMkdir(directoryPath.toFile());
    }

}

//******************************************************************************
//                  LocalFileSystemConnection.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.fs.local;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.io.FileUtils;
import org.opensilex.fs.service.FileStorageConnection;
import org.opensilex.service.BaseService;

/**
 * Local filesystem connection for file storage service
 *
 * @see org.opensilex.fs.service.FileStorageService
 * @author Vincent Migot
 */
public class LocalFileSystemConnection extends BaseService implements FileStorageConnection {

    private final Path basePath;

    public LocalFileSystemConnection() {
        super(null);
        this.basePath = null;
    }

    public LocalFileSystemConnection(Path basePath) {
        super(null);
        this.basePath = basePath;
    }

    public Path getBasePath() throws IOException {
        return this.basePath;
    }

    public Path getAbsolutePath(Path filePath) throws IOException {
        if (this.basePath == null) {
            return filePath;
        }
        return this.basePath.resolve(filePath);
    }

    public File getAbsolutePathFile(Path path) throws IOException {
        return getAbsolutePath(path).toFile();
    }

    @Override
    public String readFile(Path filePath) throws IOException {
        return FileUtils.readFileToString(getAbsolutePathFile(filePath), StandardCharsets.UTF_8);
    }

    @Override
    public byte[] readFileAsByteArray(Path filePath) throws IOException {
        return Files.readAllBytes(getAbsolutePath(filePath));
    }

    @Override
    public void writeFile(Path filePath, String content, URI fileURI) throws IOException {
        FileUtils.writeStringToFile(getAbsolutePathFile(filePath), content, StandardCharsets.UTF_8);
    }

    @Override
    public void writeFile(Path filePath, File file, URI fileURI) throws IOException {
        FileUtils.copyFile(file, getAbsolutePathFile(filePath));
    }

    @Override
    public void createDirectories(Path directoryPath) throws IOException {
        FileUtils.forceMkdir(getAbsolutePathFile(directoryPath));
    }

    @Override
    public boolean exist(Path filePath) throws IOException {
        return Files.exists(getAbsolutePath(filePath));
    }

    @Override
    public void delete(Path filePath) throws IOException {
        Files.delete(getAbsolutePath(filePath));
    }

}

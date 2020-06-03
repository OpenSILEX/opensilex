/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.fs.service;

import org.opensilex.service.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 *
 * @author vmigot
 */
public interface FileStorageConnection extends Service {

    byte[] readFileAsByteArray(Path filePath) throws IOException;

    String readFile(Path filePath) throws IOException;

    /**
     *
     * @param filePath a Path to a filesystem resource
     * @return a directly accessible ( by the local filesystem) {@link Path} to the given filePath.
     *
     * For distributed/remote filesystem, this allow to get a {@link Path} to a remote file without directly reading it,
     * for further processing (e.g. : image processing/analysis).
     *
     */
    Path getPhysicalPath(Path filePath) throws IOException;

    void writeFile(Path filePath, String content) throws IOException;
    
    void writeFile(Path filePath, File file) throws IOException;
    
    void createDirectories(Path directoryPath) throws IOException;

    Path createFile(Path filePath) throws IOException;

    boolean exist(Path filePath) throws IOException;

    void delete(Path filePath) throws IOException;
}
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.fs.service;

import org.opensilex.service.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/**
 *
 * @author vmigot
 */
public interface FileStorageConnection extends Service {

    byte[] readFileAsByteArray(Path filePath) throws IOException;

    default String readFile(Path filePath) throws IOException{
        byte[] data = readFileAsByteArray(filePath);
        return new String(data, StandardCharsets.UTF_8);
    }

    void writeFile(Path filePath,byte[] content) throws IOException;

    void writeFile(Path filePath, File file) throws IOException;

    void createDirectories(Path directoryPath) throws IOException;

    boolean exist(Path filePath) throws IOException;

    void delete(Path filePath) throws IOException;
    
    Path getAbsolutePath(Path filePath) throws IOException;
}

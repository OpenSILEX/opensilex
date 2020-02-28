/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.fs.service;

import java.io.File;
import java.nio.file.Path;
import org.opensilex.service.ServiceConnection;

/**
 *
 * @author vmigot
 */
public interface FileStorageConnection extends ServiceConnection {

    public String readFile(Path filePath) throws Exception;
    
    public void writeFile(Path filePath, String content) throws Exception;
    
    public void writeFile(Path filePath, File file) throws Exception;
    
    public void createDirectories(Path directoryPath) throws Exception;
    
}
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.fs.gridfs;

import java.io.File;
import java.nio.file.Path;
import org.opensilex.fs.service.FileStorageConnection;
import org.opensilex.service.BaseService;

/**
 *
 * @author charlero
 */
public class GridFSConnection   extends BaseService implements FileStorageConnection {

    @Override
    public String readFile(Path filePath) throws Exception {
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


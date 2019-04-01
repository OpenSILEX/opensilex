//******************************************************************************
//                            DocumentDaoMongo.java 
// SILEX-PHIS
// Copyright © - INRA - 2017
// Creation date: June 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.dao.mongo;

import com.mongodb.BasicDBObject;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSInputFile;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;
import phis2ws.service.PropertiesFileManager;
import phis2ws.service.dao.manager.DAOMongo;
import phis2ws.service.documentation.StatusCodeMsg;
import phis2ws.service.utils.POSTResultsReturn;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.model.phis.Document;

public class DocumentDaoMongo extends DAOMongo<Document> { 

    @Override
    protected BasicDBObject prepareSearchQuery() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    /**
     * 
     * @param filePath le chemin du document à enregistrer en mongo
     * @param fileUri l'uri du fichier, à mettre dans les métadonnées du document dans mongo
     * @return POSTResultsReturn
     */
    public POSTResultsReturn insertFile(String filePath, String fileUri) {
        POSTResultsReturn result = null;
        List<Status> insertStatusList = new ArrayList<>();
        try {
            File file = new File(filePath);
            
            GridFSInputFile in = gridFS.createFile(file);
            in.put("uri", fileUri);
            in.save();
            result = new POSTResultsReturn(true, true, true);
            insertStatusList.add(new Status("File saved", StatusCodeMsg.INFO, "File saved in mongodb"));
            result.statusList = insertStatusList;
            file.delete();
        } catch (IOException ex) {
            result = new POSTResultsReturn(false, false, false);
            insertStatusList.add(new Status("File exception", StatusCodeMsg.ERR, "Error while loading the file"));
            result.statusList = insertStatusList;
            Logger.getLogger(DocumentDaoMongo.class.getName()).log(Level.SEVERE, null, ex);
        }
        return result;
    }
    
    /**
     * 
     * @param documentURI l'URI du document à récupérer dans mongo
     * @return null si le document n'est pas dans la base, 
     *         le document sinon
     */
    public File getDocument(String documentURI) {
        GridFSDBFile out = (GridFSDBFile) gridFS.findOne(new BasicDBObject("uri", documentURI));
        InputStream is = out.getInputStream();
        
        File file = new File(PropertiesFileManager.getConfigFileProperty("service", "uploadFileServerDirectory") + out.get("filename"));
        
        try {
            OutputStream outputStream = new FileOutputStream(file);
            IOUtils.copy(is, outputStream);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(DocumentDaoMongo.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        } catch (IOException ex) {
            Logger.getLogger(DocumentDaoMongo.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
        
        return file;
    }

    @Override
    public List<Document> create(List<Document> objects) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void delete(List<Document> objects) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Document> update(List<Document> objects) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Document find(Document object) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Document findById(String id) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}

//******************************************************************************
//                            DocumentDaoMongo.java 
// SILEX-PHIS
// Copyright © INRA 2017
// Creation date: June 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.dao;

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
import opensilex.service.dao.exception.DAODataErrorAggregateException;
import opensilex.service.dao.exception.DAOPersistenceException;
import org.apache.commons.io.IOUtils;
import opensilex.service.PropertiesFileManager;
import opensilex.service.dao.manager.MongoDAO;
import opensilex.service.documentation.StatusCodeMsg;
import opensilex.service.utils.POSTResultsReturn;
import opensilex.service.view.brapi.Status;
import opensilex.service.model.Document;

/**
 * Documents DAO for MongoDB.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class DocumentMongoDAO extends MongoDAO<Document> { 

    @Override
    protected BasicDBObject prepareSearchQuery() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    /**
     * Inserts a file.
     * @param fileUri
     * @param file
     * @return POSTResultsReturn
     */
    public POSTResultsReturn insertFile(String fileUri, File file) {
        POSTResultsReturn result = null;
        List<Status> insertStatusList = new ArrayList<>();
        try {
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
            Logger.getLogger(DocumentMongoDAO.class.getName()).log(Level.SEVERE, null, ex);
        }
        return result;
    }
    
    /**
     * Gets a document.
     * @param documentURI
     * @return the document or null if not found
     */
    public File getDocument(String documentURI) {
        GridFSDBFile out = (GridFSDBFile) gridFS.findOne(new BasicDBObject("uri", documentURI));
        InputStream is = out.getInputStream();
        
        File file = new File(PropertiesFileManager.getConfigFileProperty("service", "uploadFileServerDirectory") + out.get("filename"));
        
        try {
            OutputStream outputStream = new FileOutputStream(file);
            IOUtils.copy(is, outputStream);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(DocumentMongoDAO.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        } catch (IOException ex) {
            Logger.getLogger(DocumentMongoDAO.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
        
        return file;
    }

    @Override
    public List<Document> create(List<Document> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void delete(List<Document> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Document> update(List<Document> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Document find(Document object) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Document findById(String id) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void validate(List<Document> objects) throws DAOPersistenceException, DAODataErrorAggregateException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}

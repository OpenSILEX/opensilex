//******************************************************************************
//                           FileInformationDAO.java
// SILEX-PHIS
// Copyright © - INRA - 2018
// Creation date: 5 Feb. 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.dao;

import com.mongodb.BasicDBObject;
import java.util.List;
import opensilex.service.dao.exception.DAODataErrorAggregateException;
import opensilex.service.dao.exception.DAOPersistenceException;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import opensilex.service.dao.manager.MongoDAO;
import opensilex.service.model.FileInformations;

/**
 * File information DAO.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class FileInformationDAO extends MongoDAO<FileInformations> {
    
    final static Logger LOGGER = LoggerFactory.getLogger(FileInformationDAO.class);
    
    /**
     * Represents the MongoDB documents label for the file extension
     */
    final static String DB_FIELDS_EXTENSION = "extension";
    
    /**
     * Represents the MongoDB documents label for the file md5sum
     */
    final static String DB_FIELDS_MD5SUM = "md5sum";
    
    /**
     * Represents the MongoDB documents label for the server file path
     */
    final static String DB_FIELDS_SERVER_FILE_PATH = "serverFilePath";

    @Override
    protected BasicDBObject prepareSearchQuery() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    /**
     * Gets a document corresponding to an image shooting configuration and 
     * transforms it in a ShootingConfiguration object.
     * @param fileInformationsDocument
     * @return the Shooting configuration corresponding to the document
     */
    public static FileInformations mongoDocumentToFileInformation(Document fileInformationsDocument) {
        FileInformations fileInformations = new FileInformations();
        fileInformations.setChecksum(fileInformationsDocument.getString(FileInformationDAO.DB_FIELDS_MD5SUM));
        fileInformations.setExtension(fileInformationsDocument.getString(FileInformationDAO.DB_FIELDS_EXTENSION));
        fileInformations.setServerFilePath(fileInformationsDocument.getString(FileInformationDAO.DB_FIELDS_SERVER_FILE_PATH));
        
        return fileInformations;
    }

    @Override
    public List<FileInformations> create(List<FileInformations> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void delete(List<FileInformations> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<FileInformations> update(List<FileInformations> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public FileInformations find(FileInformations object) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public FileInformations findById(String id) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void validate(List<FileInformations> objects) throws DAOPersistenceException, DAODataErrorAggregateException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}

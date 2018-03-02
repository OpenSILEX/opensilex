//******************************************************************************
//                                       FileInformationsDAOMongo.java
//
// Author(s): Morgane Vidal <morgane.vidal@inra.fr>
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: 5 févr. 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  5 févr. 2018
// Subject: A dao specific to images metadata file informations
//******************************************************************************
package phis2ws.service.dao.mongo;

import com.mongodb.BasicDBObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.configuration.DateFormats;
import phis2ws.service.dao.manager.DAOMongo;
import phis2ws.service.view.model.phis.FileInformations;

/**
 * Represents the mongodb Data Access Object for the images metadata 
 * file informations
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class FileInformationsDAOMongo extends DAOMongo<FileInformations> {
    
    final static Logger LOGGER = LoggerFactory.getLogger(FileInformationsDAOMongo.class);
    
    //Represents the mongodb documents label for the file extension
    final static String DB_FIELDS_EXTENSION = "extension";
    //Represents the mongodb documents label for the file md5sum
    final static String DB_FIELDS_MD5SUM = "md5sum";
    //Represents the mongodb documents label for the server file path
    final static String DB_FIELDS_SERVER_FILE_PATH = "serverFilePath";

    @Override
    protected BasicDBObject prepareSearchQuery() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ArrayList<FileInformations> allPaginate() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    /**
     * get a Document corresponding to an image shooting configuration and 
     * transform it in a ShootingConfiguration object
     * @param fileInformationsDocument
     * @return the Shooting configuration corresponding to the document
     */
    public static FileInformations mongoDocumentToFileInformation(Document fileInformationsDocument) {
        FileInformations fileInformations = new FileInformations();
        fileInformations.setChecksum(fileInformationsDocument.getString(FileInformationsDAOMongo.DB_FIELDS_MD5SUM));
        fileInformations.setExtension(fileInformationsDocument.getString(FileInformationsDAOMongo.DB_FIELDS_EXTENSION));
        fileInformations.setServerFilePath(fileInformationsDocument.getString(FileInformationsDAOMongo.DB_FIELDS_SERVER_FILE_PATH));
        
        return fileInformations;
    }
}

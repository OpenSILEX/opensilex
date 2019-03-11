//******************************************************************************
//                                       DataFileDAOMongo.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 7 mars 2019
// Contact: Expression userEmail is undefined on line 6, column 15 in file:///home/vincent/opensilex/phis-ws/phis2-ws/licenseheader.txt., anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.dao.mongo;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoException;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import java.io.File;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import javax.ws.rs.core.Response;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.PropertiesFileManager;
import phis2ws.service.dao.manager.DAOMongo;
import phis2ws.service.dao.sesame.ScientificObjectDAOSesame;
import phis2ws.service.documentation.StatusCodeMsg;
import phis2ws.service.ontologies.Oeso;
import phis2ws.service.utils.FileUploader;
import phis2ws.service.utils.POSTResultsReturn;
import phis2ws.service.utils.UriGenerator;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.model.phis.ConcernedItem;
import phis2ws.service.view.model.phis.FileDescription;

/**
 *
 * @author vincent
 */
public class DataFileDAOMongo extends DAOMongo<FileDescription> {
    
    private final static Logger LOGGER = LoggerFactory.getLogger(DataFileDAOMongo.class);
    
    private final static String DB_FIELD_URI = "uri";
    private final static String DB_FIELD_RDF_TYPE = "rdfType";
    private final static String DB_FIELD_FILE_NAME = "fileName";
    private final static String DB_FIELD_CONCERNED_ITEMS = "concernedItems";
    private final static String DB_FIELD_DATE = "date";
    private final static String DB_FIELD_PROVENANCE = "provenance";
    private final static String DB_FIELD_METADATA = "metadata";

    @Override
    protected BasicDBObject prepareSearchQuery() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ArrayList<FileDescription> allPaginate() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * Check and insert data file and its metadata
     * @param fileDescription
     * @param file
     * @return true if file and its metadata are saved
     *         false in case of errors
     */
    public POSTResultsReturn checkAndInsert(FileDescription fileDescription, File file) {
        POSTResultsReturn checkResult = check(fileDescription);
        if (checkResult.getDataState()) {
            return insert(fileDescription, file);
        } else { //Errors in the data
            return checkResult;
        }
    }
    
    /**
     * Check the given data file description.
     *
     * @param fileDescription
     * @return the check result with the founded errors
     */
    private POSTResultsReturn check(FileDescription fileDescription) {
        POSTResultsReturn checkResult = new POSTResultsReturn();
        List<Status> checkStatus = new ArrayList<>();

        boolean dataOk = true;

        ProvenanceDAOMongo provenanceDAO = new ProvenanceDAOMongo();
        ScientificObjectDAOSesame scientificObjectDao = new ScientificObjectDAOSesame();
        
        if (!provenanceDAO.existProvenanceUri(fileDescription.getProvenanceUri())) {
            // 1. Check if the provenance uri exist and is a provenance
            dataOk = false;
            checkStatus.add(new Status(StatusCodeMsg.WRONG_VALUE, StatusCodeMsg.ERR, 
                "Unknwon provenance : " + fileDescription.getProvenanceUri()));
        } else if (!scientificObjectDao.existUri(fileDescription.getRdfType())) {
            // 2. Check if the rdf type uri exist, 
            // we use scientificObjectDao for convenience to access DaoSesame.existUri method
            dataOk = false;
            checkStatus.add(new Status(StatusCodeMsg.WRONG_VALUE, StatusCodeMsg.ERR, 
                "Unknwon file rdf type : " + fileDescription.getRdfType()));
        } else {
            // 3. Check concerned items consistency
            for(ConcernedItem concernedItem : fileDescription.getConcernedItems()) {
                if (!scientificObjectDao.existScientificObject(concernedItem.getUri())) {
                    dataOk = false;
                    checkStatus.add(new Status(StatusCodeMsg.WRONG_VALUE, StatusCodeMsg.ERR, 
                        "Unknwon concerned item : " + concernedItem.getUri()));
                }
                if (!scientificObjectDao.existUri(concernedItem.getRdfType())) {
                    dataOk = false;
                    checkStatus.add(new Status(StatusCodeMsg.WRONG_VALUE, StatusCodeMsg.ERR, 
                        "Unknwon concerned item type : " + concernedItem.getUri()));
                }
            }
        }
        
        checkResult = new POSTResultsReturn(dataOk, null, dataOk);
        checkResult.statusList = checkStatus;
        return checkResult;
    }
    
    /**
     * Insert the given data in the mongodb database
     *
     * @param dataList
     * @return the insertion result
     */
    private POSTResultsReturn insert(FileDescription fileDescription, File file) {
        // 1. Initialize transaction
        MongoClient client = DAOMongo.getMongoClient();
        ClientSession session = client.startSession();
        session.startTransaction();

        POSTResultsReturn result = null;
        List<Status> status = new ArrayList<>();
        List<String> createdResources = new ArrayList<>();

        // 2. Create unique index on uri for file rdf type collection
        //   Mongo won't create index if it already exists
        IndexOptions indexOptions = new IndexOptions().unique(true);
        String fileCollectionName = getCollectionFromFileType(fileDescription.getRdfType());
        MongoCollection<Document> fileDescriptionCollection = database.getCollection(fileCollectionName);
        fileDescriptionCollection.createIndex(new BasicDBObject(DB_FIELD_URI, 1), indexOptions);
        
        boolean hasError = false;
            
        FileUploader uploader = new FileUploader();
        try {
            //3. Insert metadata first
            Document dataToInsert = prepareInsertFileDescriptionDocument(fileDescription);
            fileDescriptionCollection.insertOne(session, dataToInsert);

            //4. Copy file to directory
            final String filename =  Base64.getEncoder().encodeToString(fileDescription.getUri().getBytes());
            final String fileServerDirectory = PropertiesFileManager.getConfigFileProperty("service", "uploadFileServerDirectory") 
                        + "/dataFiles/"
                        + fileCollectionName + "/";
            
            uploader.createNestedDirectories(fileServerDirectory);
            
            ChannelSftp channel = uploader.getChannelSftp();
            channel.stat(fileServerDirectory);
            channel.cd(fileServerDirectory);
        
            if (uploader.fileTransfer(file, fileServerDirectory + filename)) { 
                status.add(new Status(
                    StatusCodeMsg.RESOURCES_CREATED,
                    StatusCodeMsg.INFO,
                    StatusCodeMsg.DATA_INSERTED + " for the file " + fileDescription.getFilename()
                ));
                            
                createdResources.add(fileDescription.getUri());                
            } else {
                status.add(new Status(
                        StatusCodeMsg.UNEXPECTED_ERROR,
                        StatusCodeMsg.ERR,
                        "An error occurred during file upload, try to submit it again."
                ));
                hasError = true;
            }
            
        } catch (MongoException ex) {
            // Define that an error occurs
            hasError = true;

            // Error check if it's because of a duplicated data error
            // Add status according to the error type (duplication or unexpected)
            if (ex.getCode() == DAOMongo.DUPLICATE_KEY_ERROR_CODE) {
                status.add(new Status(
                        StatusCodeMsg.ALREADY_EXISTING_DATA,
                        StatusCodeMsg.ERR,
                        ex.getMessage()
                ));
            } else {
                // Add the original exception message for debugging
                status.add(new Status(
                        StatusCodeMsg.UNEXPECTED_ERROR,
                        StatusCodeMsg.ERR,
                        StatusCodeMsg.DATA_REJECTED + " for the file " + file.getName() + " - " + ex.getMessage()
                ));
            }
        } catch (SftpException ex) {
            status.add(new Status(
                    StatusCodeMsg.UNEXPECTED_ERROR,
                    StatusCodeMsg.ERR,
                    "An error occurred during file upload, try to submit it again: " + ex.getMessage()
            ));
            hasError = true;
        } finally {
            uploader.closeConnection();
        }

        // 5. Prepare result to return
        result = new POSTResultsReturn(hasError);
        result.statusList = status;

        if (!hasError) {
            // If no errors commit transaction
            session.commitTransaction();
            result.setHttpStatus(Response.Status.CREATED);
            result.createdResources = createdResources;
        } else {
            // If errors abort transaction
            session.abortTransaction();
            result.setHttpStatus(Response.Status.INTERNAL_SERVER_ERROR);
        }

        // Close transaction session
        session.close();
        return result;
    }

    /**
     * Return collection name from file rdt type uri
     * @param rdfType
     * @return 
     */
    private String getCollectionFromFileType(String rdfType) {
        String[] split = rdfType.split("#");
        return split[split.length - 1];
    }

    /**
     * Prepare mongodb document to insert from filedescription
     * @param fileDescription
     * @return 
     */
    private Document prepareInsertFileDescriptionDocument(FileDescription fileDescription) {
        Document document = new Document();

        try {
            UriGenerator uriGenerator = new UriGenerator();
            
            String key = fileDescription.getFilename() + fileDescription.getDate();
            String uri = uriGenerator.generateNewInstanceUri(Oeso.CONCEPT_DATA_FILE.toString(), null, key);
            while (uriExists(fileDescription.getRdfType(), uri)) {
                uri = uriGenerator.generateNewInstanceUri(Oeso.CONCEPT_DATA_FILE.toString(), null, key);
            }
            
            fileDescription.setUri(uri);
            
            document.append(DB_FIELD_URI, fileDescription.getUri());
            document.append(DB_FIELD_FILE_NAME, fileDescription.getFilename());
            document.append(DB_FIELD_RDF_TYPE, fileDescription.getRdfType());
            document.append(DB_FIELD_CONCERNED_ITEMS, fileDescription.getConcernedItems());
            document.append(DB_FIELD_DATE, fileDescription.getDate());
            document.append(DB_FIELD_PROVENANCE, fileDescription.getProvenanceUri());
            document.append(DB_FIELD_METADATA, fileDescription.getMetadata());

            LOGGER.debug(document.toJson());
        } catch (Exception e) {
            LOGGER.error("Exception while generating uri, should never append", e);
        }
        
        return document;
    }

    /**
     * Return true if the given URI already exists in variable collection
     * @param variableUri variable which will determine in which collection to look
     * @param uri URI to check
     * @return true if the URI exists and false otherwise
     */
    public boolean uriExists(String rdfType, String uri) {
        String variableCollection = getCollectionFromFileType(rdfType);
        
        BasicDBObject query = new BasicDBObject();
        query.append(DB_FIELD_URI, uri);
        
        return database.getCollection(variableCollection).countDocuments(query) > 0; 
    }
}
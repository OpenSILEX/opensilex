//**********************************************************************************************
//                                       ImageMetadataDaoSesame.java 
//
// Author(s): Morgane Vidal
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: December, 11 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  December, 11 2017
// Subject: A Dao specific to insert image metadata in mongodb
//***********************************************************************************************
package phis2ws.service.dao.mongo;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Year;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import javax.ws.rs.core.Response;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.PropertiesFileManager;
import phis2ws.service.configuration.DateFormats;
import phis2ws.service.configuration.URINamespaces;
import phis2ws.service.dao.manager.DAOMongo;
import phis2ws.service.dao.sesame.ImageMetadataDaoSesame;
import phis2ws.service.documentation.StatusCodeMsg;
import phis2ws.service.resources.dto.ConcernItemDTO;
import phis2ws.service.resources.dto.ImageMetadataDTO;
import phis2ws.service.resources.dto.manager.AbstractVerifiedClass;
import phis2ws.service.utils.POSTResultsReturn;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.model.phis.ConcernItem;
import phis2ws.service.view.model.phis.ImageMetadata;

/**
 * Represents the MongoDB Data Access Object for the images metadata
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ImageMetadataDaoMongo extends DAOMongo<ImageMetadata> {

    final static Logger LOGGER = LoggerFactory.getLogger(ImageMetadataDaoMongo.class);
    public String uri;
    public String rdfType;
    //Date of shooting
    public String date;
    //List of the elements concerned by the image. The elements are represented 
    //by uris
    public ArrayList<String> concernedItems = new ArrayList<>();
    
    private final MongoCollection<Document> imagesCollection = database.getCollection(PropertiesFileManager.getConfigFileProperty("mongodb_nosql_config", "images"));
    
    //Represents the mongodb documents label for the image uri
    final static String DB_FIELDS_IMAGE_URI = "uri";
    //Represents the mongodb documents label for the concerned items uris
    final static String DB_FIELDS_CONCERNED_ITEM_URI = "uri";
    //Represents the mongodb documents label for the rdf types
    final static String DB_FIELDS_RDF_TYPE = "rdfType";
    //Represents the mongodb documents label for the concerned items list
    final static String DB_FIELDS_CONCERN = "concern";
    //Represents the mongodb documents label for the shooting configurations
    final static String DB_FIELDS_SHOOTING_CONFIGURATION = "shootingConfiguration";
        //Represents the mongodb documents label for the storage
    final static String DB_FIELDS_STORAGE = "storage";
    
    

    public ImageMetadataDaoMongo() {
        super();
    }
    
    /**
     * search images metadata by uri, rdfType, date, concernedItems
     * @return the search query.
     *         Query example :
     *         { 
     *              "uri" : "http://www.phenome-fppn.fr/phis_field/2017/i170000000000" , 
     *              "rdfType" : "http://www.phenome-fppn.fr/vocabulary/2017#HemisphericalImage" , 
     *              "$and" : 
     *                  [ 
     *                      { 
     *                          "concern" : { 
     *                              "$elemMatch" : { 
     *                                  "uri" : "http://phenome-fppn.fr/phis_field/ao1"
     *                              }
     *                          }
     *                      },
     *                      { 
     *                          "concern" : { 
     *                              "$elemMatch" : { 
     *                                  "uri" : "http://phenome-fppn.fr/phis_field/ao1"
     *                              }
     *                          }
     *                      }
     *                  ] , 
     *                  "shootingConfiguration.date" : { 
     *                      "$date" : "2017-06-15T08:51:00.000Z"
     *                  }
     *              }
     */
    @Override
    protected BasicDBObject prepareSearchQuery() {
       BasicDBObject query = new BasicDBObject();
       
       if (uri != null) {
           query.append(DB_FIELDS_IMAGE_URI, uri);
       }
       if (rdfType != null) {
           query.append(DB_FIELDS_RDF_TYPE, rdfType);
       }
       if (concernedItems != null && !concernedItems.isEmpty()) {
           BasicDBList and = new BasicDBList();
           for (String concernedItem : concernedItems) {
               BasicDBObject clause = new BasicDBObject(DB_FIELDS_CONCERN, new BasicDBObject("$elemMatch", new BasicDBObject(DB_FIELDS_CONCERNED_ITEM_URI, concernedItem)));
               and.add(clause);
           }
           
           query.append("$and", and);
       }
       if (date != null) {
           try {
               SimpleDateFormat df = new SimpleDateFormat(DateFormats.YMDHMSZ_FORMAT);
               Date dateSearch = df.parse(date);
               query.append(DB_FIELDS_SHOOTING_CONFIGURATION + "." + ShootingConfigurationDAOMongo.DB_FIELDS_DATE, dateSearch);
           } catch (ParseException ex) {
               java.util.logging.Logger.getLogger(ImageMetadataDaoMongo.class.getName()).log(Level.SEVERE, null, ex);
           }
       }
       
       return query;
    }
    
    /**
     * transform a documents list (which is supposed to contains concerned items)
     * into a concerned items list
     * @param concernedItemsDocuments
     * @return the concerned items extracted from the document list
     */
    private ArrayList<ConcernItem> mongoDocumentListToConcernedItems(List<Document> concernedItemsDocuments) {
        ArrayList<ConcernItem> concernedItemsToReturn = new ArrayList<>();
        for (Document concernedItemDocument : concernedItemsDocuments) {
            ConcernItem concernedItem = new ConcernItem();
            concernedItem.setUri(concernedItemDocument.getString(DB_FIELDS_CONCERNED_ITEM_URI));
            concernedItem.setRdfType(concernedItemDocument.getString(DB_FIELDS_RDF_TYPE));
            concernedItemsToReturn.add(concernedItem);
        }
        return concernedItemsToReturn;
    }

    @Override
    public ArrayList<ImageMetadata> allPaginate() {
        BasicDBObject searchQuery = prepareSearchQuery();
        LOGGER.debug(getTraceabilityLogs() + " query : " + searchQuery.toString());
        
        FindIterable<Document> imagesMetadataMongo = imagesCollection.find(searchQuery);
        
        ArrayList<ImageMetadata> imagesMetadata = new ArrayList<>();
        
        try (MongoCursor<Document> imagesMetadataCursor = imagesMetadataMongo.iterator()) {
            //for each found image metadata, 
            //add the image in the ArrayList<ImageMetadata> to return
            while (imagesMetadataCursor.hasNext()) {
                Document imageMetadataDocument = imagesMetadataCursor.next();
                
                ImageMetadata imageMetadata = new ImageMetadata();
                imageMetadata.setUri(imageMetadataDocument.getString(DB_FIELDS_IMAGE_URI));
                imageMetadata.setRdfType(imageMetadataDocument.getString(DB_FIELDS_RDF_TYPE));
                
                //Add the elements concerned by the image in the ImageMetadata
                List<Document> concernedItemDocuments = (List<Document>) imageMetadataDocument.get(DB_FIELDS_CONCERN);
                imageMetadata.setConcernedItems(mongoDocumentListToConcernedItems(concernedItemDocuments));
                
                //Add the shootingConfiguration
                Document shootingConfigurationDocument = (Document) imageMetadataDocument.get(DB_FIELDS_SHOOTING_CONFIGURATION);
                imageMetadata.setConfiguration(ShootingConfigurationDAOMongo.mongoDocumentToShootingConfiguration(shootingConfigurationDocument));
                
                //add the file informations
                Document fileInformationsDocument = (Document) imageMetadataDocument.get(DB_FIELDS_STORAGE);
                imageMetadata.setFileInformations(FileInformationsDAOMongo.mongoDocumentToFileInformation(fileInformationsDocument));
                
                imagesMetadata.add(imageMetadata);
            }
        }
        
        return imagesMetadata;
    }
    
    /**
     * check if the images metadata are correct 
     * (rules, image type, concerned items)
     * @see phis2ws.service.resources.dto rules()
     * @param imagesMetadata
     * @return the result of the check of the images metadata. 
     */
    public POSTResultsReturn check(List<ImageMetadataDTO> imagesMetadata) {
        List<Status> checkStatusList = new ArrayList<>(); //Status to be returned
        
        boolean dataOk = true;
        
        for (ImageMetadataDTO imageMetadata : imagesMetadata) {
            if ((boolean) imageMetadata.isOk().get(AbstractVerifiedClass.STATE)) { //Metadata correct
                //1. Check if the image type exist
                ImageMetadataDaoSesame imageMetadataDaoSesame = new ImageMetadataDaoSesame();
                if (!imageMetadataDaoSesame.existObject(imageMetadata.getRdfType())) {
                    dataOk = false;
                    checkStatusList.add(new Status(StatusCodeMsg.WRONG_VALUE, StatusCodeMsg.ERR, "Wrong image type given : " + imageMetadata.getRdfType()));
                }
                
                //2. Check if the concerned items exist in the triplestore
                for (ConcernItemDTO concernedItem : imageMetadata.getConcern()) {
                    if (!imageMetadataDaoSesame.existObject(concernedItem.getUri())) {
                        dataOk = false;
                        checkStatusList.add(new Status(StatusCodeMsg.WRONG_VALUE, StatusCodeMsg.ERR, "Unknown concerned item given : " + concernedItem.getUri()));
                    }
                }
            } else {
                dataOk = false;
                checkStatusList.add(new Status(StatusCodeMsg.BAD_DATA_FORMAT, StatusCodeMsg.ERR, 
                        new StringBuilder().append(StatusCodeMsg.MISSING_FIELDS_LIST).append(imageMetadata.isOk()).toString()));
            }
        }
        
        POSTResultsReturn imagesMetadataCheck = new POSTResultsReturn(dataOk, null, dataOk);
        imagesMetadataCheck.statusList = checkStatusList;
        return imagesMetadataCheck;
    }
    
    /**
     * Prepare the query of getting the number of images by year.
     * @return query to get the number of images by year. 
     *         Query example : 
     *         {uri: {$regex: "http://www.phenome-fppn.fr/diaphen/2017*"}}    
     */
    private Document prepareGetNbImagesYearQuery() {        
          Document regQuery = new Document();
          URINamespaces uriNamespaces = new URINamespaces();
          String regex = uriNamespaces.getContextsProperty("pxPlatform") + "/" + Year.now().toString() + "*";
          regQuery.append("$regex", regex);
          
          Document findQuery = new Document();
          findQuery.append(DB_FIELDS_IMAGE_URI, regQuery);
        
          return findQuery;
    }
    
    /**
     * 
     * @return the number of images in the database for the actual year
     */
    public long getNbImagesYear() {
        Document query = prepareGetNbImagesYearQuery();
        
        LOGGER.debug(getTraceabilityLogs() + " query : " + query.toString());
        return imagesCollection.count(query);
    }
    
    /**
     * insert the images metadata in the nosql database
     * @param imagesMetadata
     * @return the result of the insert
     * @throws ParseException 
     */
    @SuppressWarnings("empty-statement")
    public POSTResultsReturn insert(List<ImageMetadata> imagesMetadata) throws ParseException {
        List<Status> insertStatus = new ArrayList<>();
        POSTResultsReturn result;
        List<String> createdResourcesUris = new ArrayList<>();
        
       //SILEX:todo
       //transactions
       //\SILEX:todo
       SimpleDateFormat df = new SimpleDateFormat(DateFormats.YMDHMSZ_FORMAT);
       for (ImageMetadata imageMetadata : imagesMetadata) {
           Document metadata = new Document();
           metadata.append(DB_FIELDS_IMAGE_URI, imageMetadata.getUri());
           metadata.append(DB_FIELDS_RDF_TYPE, imageMetadata.getRdfType());
           
           //Concern
           ArrayList<Document> concernedItemsToSave = new ArrayList<>();
           for (ConcernItem concernItem : imageMetadata.getConcernedItems()) {
               Document concern = new Document();
               concern.append(DB_FIELDS_CONCERNED_ITEM_URI, concernItem.getUri());
               concern.append(DB_FIELDS_RDF_TYPE, concernItem.getRdfType());
               concernedItemsToSave.add(concern);
           }
           metadata.append(DB_FIELDS_CONCERN, concernedItemsToSave);
           
           //Configuration
           Document configuration = new Document();
           Date dateImage = df.parse(imageMetadata.getConfiguration().getDate());
           configuration.append(ShootingConfigurationDAOMongo.DB_FIELDS_DATE, dateImage);
           Timestamp timestamp = new Timestamp(new Date().getTime());
           configuration.append(ShootingConfigurationDAOMongo.DB_FIELDS_TIMESTAMP, timestamp.getTime());
           configuration.append(ShootingConfigurationDAOMongo.DB_FIELDS_SENSOR_POSITION, imageMetadata.getConfiguration().getPosition());
           metadata.append(DB_FIELDS_SHOOTING_CONFIGURATION, configuration);
           
           //FileInformations (Storage)
           Document storage = new Document();
           storage.append(FileInformationsDAOMongo.DB_FIELDS_EXTENSION, imageMetadata.getFileInformations().getExtension());
           storage.append(FileInformationsDAOMongo.DB_FIELDS_MD5SUM, imageMetadata.getFileInformations().getChecksum());
           storage.append(FileInformationsDAOMongo.DB_FIELDS_SERVER_FILE_PATH, imageMetadata.getFileInformations().getServerFilePath());
           metadata.append(DB_FIELDS_STORAGE, storage);
           
           LOGGER.debug("MongoDB insert : " + metadata.toJson());
           imagesCollection.insertOne(metadata);
           createdResourcesUris.add(imageMetadata.getUri());
       }
       
       insertStatus.add(new Status(StatusCodeMsg.RESOURCES_CREATED, StatusCodeMsg.INFO, StatusCodeMsg.DATA_INSERTED));;
       result = new POSTResultsReturn(Boolean.TRUE, Boolean.TRUE, Boolean.TRUE);
       result.setHttpStatus(Response.Status.CREATED);
       result.statusList = insertStatus;
       result.createdResources = createdResourcesUris;
       
       return result;
    }
}

//**********************************************************************************************
//                                       ImageDaoSesame.java 
//
// Author(s): Morgane VIDAL
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: December, 11 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  December, 11 2017
// Subject: A Dao specific to images insert into mongodb
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
import phis2ws.service.configuration.URINamespaces;
import phis2ws.service.dao.manager.DAOMongo;
import phis2ws.service.dao.sesame.ImageMetadataDaoSesame;
import phis2ws.service.documentation.StatusCodeMsg;
import phis2ws.service.resources.dto.ConcernItemDTO;
import phis2ws.service.resources.dto.ImageMetadataDTO;
import phis2ws.service.utils.POSTResultsReturn;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.model.phis.ConcernItem;
import phis2ws.service.view.model.phis.FileInformations;
import phis2ws.service.view.model.phis.ImageMetadata;
import phis2ws.service.view.model.phis.ShootingConfiguration;

/**
 * Represents the MongoDB Data Access Object for the images
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ImageMetadataDaoMongo extends DAOMongo<ImageMetadata> {

    final static Logger LOGGER = LoggerFactory.getLogger(ImageMetadataDaoMongo.class);
    public String uri;
    public String rdfType;
    public String date;
    public ArrayList<String> concernedItems = new ArrayList<>();
    
    private final MongoCollection<Document> imagesCollection = database.getCollection(PropertiesFileManager.getConfigFileProperty("mongodb_nosql_config", "images"));

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
           query.append("uri", uri);
       }
       if (rdfType != null) {
           query.append("rdfType", rdfType);
       }
       if (concernedItems != null && !concernedItems.isEmpty()) {
           BasicDBList and = new BasicDBList();
           for (String concernedItem : concernedItems) {
               BasicDBObject clause = new BasicDBObject("concern", new BasicDBObject("$elemMatch", new BasicDBObject("uri", concernedItem)));
               and.add(clause);
           }
           query.append("$and", and);
       }
       if (date != null) {
           try {
               SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");
               Date dateSearch = df.parse(date);
               query.append("shootingConfiguration.date", dateSearch);
           } catch (ParseException ex) {
               java.util.logging.Logger.getLogger(ImageMetadataDaoMongo.class.getName()).log(Level.SEVERE, null, ex);
           }
       }
       
       return query;
    }

    @Override
    public ArrayList<ImageMetadata> allPaginate() {
        BasicDBObject query = prepareSearchQuery();
        LOGGER.trace(getTraceabilityLogs() + " query : " + query.toString());
        
        FindIterable<Document> imagesMetadataMongo = imagesCollection.find(query);
        
        ArrayList<ImageMetadata> imagesMetadata = new ArrayList<>();
        
        try (MongoCursor<Document> imagesMetadataCursor = imagesMetadataMongo.iterator()) {
            while (imagesMetadataCursor.hasNext()) {
                Document imageMetadataDocument = imagesMetadataCursor.next();
                
                ImageMetadata imageMetadata = new ImageMetadata();
                imageMetadata.setUri(imageMetadataDocument.getString("uri"));
                imageMetadata.setRdfType(imageMetadataDocument.getString("rdfType"));
                
                List<Document> concernedItemDocuments = (List<Document>) imageMetadataDocument.get("concern");
                for (Document concernedItemDocument : concernedItemDocuments) {
                    ConcernItem concernedItem = new ConcernItem();
                    concernedItem.setUri(concernedItemDocument.getString("uri"));
                    concernedItem.setRdfType(concernedItemDocument.getString("rdfType"));
                    imageMetadata.addConcernedItem(concernedItem);
                }
                
                Document shootingConfigurationDocument = (Document) imageMetadataDocument.get("shootingConfiguration");
                ShootingConfiguration shootingConfiguration = new ShootingConfiguration();
                shootingConfiguration.setDate(new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ").format(shootingConfigurationDocument.getDate("date")));
                shootingConfiguration.setPosition(shootingConfigurationDocument.getString("sensorPosition"));
                shootingConfiguration.setTimestamp(Long.toString(shootingConfigurationDocument.getLong("timestamp")));
                imageMetadata.setConfiguration(shootingConfiguration);
                
                Document fileInformationsDocument = (Document) imageMetadataDocument.get("storage");
                FileInformations fileInformations = new FileInformations();
                fileInformations.setChecksum(fileInformationsDocument.getString("md5sum"));
                fileInformations.setExtension(fileInformationsDocument.getString("extension"));
                fileInformations.setServerFilePath(fileInformationsDocument.getString("serverFilePath"));
                imageMetadata.setFileInformations(fileInformations);
                
                imagesMetadata.add(imageMetadata);
            }
        }
        
        return imagesMetadata;
    }
    
    /**
     * check if the images metadata are correct.
     * @param imagesMetadata
     * @return the result of the check of the images metadata. 
     */
    public POSTResultsReturn check(List<ImageMetadataDTO> imagesMetadata) {
        List<Status> checkStatusList = new ArrayList<>(); //Status to be returned
        
        boolean dataOk = true;
        
        for (ImageMetadataDTO imageMetadata : imagesMetadata) {
            if ((boolean) imageMetadata.isOk().get("state")) { //Metadata correct
                //1. Check if the image type exist
                ImageMetadataDaoSesame imageMetadataDaoSesame = new ImageMetadataDaoSesame();
                if (!imageMetadataDaoSesame.existObject(imageMetadata.getRdfType())) {
                    dataOk = false;
                    checkStatusList.add(new Status("Wrong value", StatusCodeMsg.ERR, "Wrong image type given : " + imageMetadata.getRdfType()));
                }
                
                //2. Check if the concerned items exist in the triplestore
                for (ConcernItemDTO concernedItem : imageMetadata.getConcern()) {
                    if (!imageMetadataDaoSesame.existObject(concernedItem.getUri())) {
                        dataOk = false;
                        checkStatusList.add(new Status("Wrong value", StatusCodeMsg.ERR, "Unknown concerned item given : " + concernedItem.getUri()));
                    }
                }
            } else {
                dataOk = false;
                checkStatusList.add(new Status("Bad data format", StatusCodeMsg.ERR, 
                        new StringBuilder().append(StatusCodeMsg.MISSINGFIELDS).append(imageMetadata.isOk()).toString()));
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
          findQuery.append("uri", regQuery);
        
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
       SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");
       for (ImageMetadata imageMetadata : imagesMetadata) {
           Document metadata = new Document();
           metadata.append("uri", imageMetadata.getUri());
           metadata.append("rdfType", imageMetadata.getRdfType());
           
           //Concern
           ArrayList<Document> concernedItemsToSave = new ArrayList<>();
           for (ConcernItem concernItem : imageMetadata.getConcernedItems()) {
               Document concern = new Document();
               concern.append("uri", concernItem.getUri());
               concern.append("rdfType", concernItem.getRdfType());
               concernedItemsToSave.add(concern);
           }
           metadata.append("concern", concernedItemsToSave);
           
           //Configuration
           Document configuration = new Document();
           Date dateImage = df.parse(imageMetadata.getConfiguration().getDate());
           configuration.append("date", dateImage);
           Timestamp timestamp = new Timestamp(new Date().getTime());
           configuration.append("timestamp", timestamp.getTime());
           configuration.append("sensorPosition", imageMetadata.getConfiguration().getPosition());
           metadata.append("shootingConfiguration", configuration);
           
           //FileInformations (Storage)
           Document storage = new Document();
           storage.append("extension", imageMetadata.getFileInformations().getExtension());
           storage.append("md5sum", imageMetadata.getFileInformations().getChecksum());
           storage.append("serverFilePath", imageMetadata.getFileInformations().getServerFilePath());
           metadata.append("storage", storage);
           
           LOGGER.debug("MongoDB insert : " + metadata.toJson());
           imagesCollection.insertOne(metadata);
           createdResourcesUris.add(imageMetadata.getUri());
       }
       
       insertStatus.add(new Status("Resource created", StatusCodeMsg.INFO, "images metadata inserted"));;
       result = new POSTResultsReturn(Boolean.TRUE, Boolean.TRUE, Boolean.TRUE);
       result.setHttpStatus(Response.Status.CREATED);
       result.statusList = insertStatus;
       result.createdResources = createdResourcesUris;
       
       return result;
    }
}

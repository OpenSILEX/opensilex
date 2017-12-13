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

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Year;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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
import phis2ws.service.view.model.phis.ImageMetadata;

public class ImageMetadataDaoMongo extends DAOMongo<ImageMetadata>{

    final static Logger LOGGER = LoggerFactory.getLogger(ImageMetadataDaoMongo.class);
    
    private final MongoCollection<Document> imagesCollection = database.getCollection(PropertiesFileManager.getConfigFileProperty("mongodb_nosql_config", "images"));

    public ImageMetadataDaoMongo() {
        super();
    }
    
    @Override
    protected BasicDBObject prepareSearchQuery() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ArrayList<ImageMetadata> allPaginate() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    public POSTResultsReturn check(List<ImageMetadataDTO> imagesMetadata) {
        List<Status> checkStatusList = new ArrayList<>(); //Liste des status retrounés
        
        boolean dataOk = true;
        
        for (ImageMetadataDTO imageMetadata : imagesMetadata) {
            if ((boolean) imageMetadata.isOk().get("state")) { //Données reçues correctes
                //1. Vérification de l'existance du type d'image
                ImageMetadataDaoSesame imageMetadataDaoSesame = new ImageMetadataDaoSesame();
                if (!imageMetadataDaoSesame.existObject(imageMetadata.getRdfType())) {
                    dataOk = false;
                    checkStatusList.add(new Status("Wrong value", StatusCodeMsg.ERR, "Wrong image type given : " + imageMetadata.getRdfType()));
                }
                
                //2. Vérification de l'existance des entités concernées, dans le triplestore (?)
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
     * @return un entier correspondant au nombre d'images dans le système pour cette année.
     */
    public long getNbImagesYear() {
        Document query = prepareGetNbImagesYearQuery();
        
        LOGGER.trace(getTraceabilityLogs() + " query : " + query.toString());
        return imagesCollection.count(query);
    }
    
    public POSTResultsReturn insert(List<ImageMetadata> imagesMetadata) throws ParseException {
        List<Status> insertStatus = new ArrayList<>();
        POSTResultsReturn result;
        List<String> createdResourcesUris = new ArrayList<>();
        
       //SILEX:todo
       //transactions
       //\SILEX:todo
//       SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.Z");
       SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");
       for (ImageMetadata imageMetadata : imagesMetadata) {
           Document metadata = new Document();
           metadata.append("uri", imageMetadata.getUri());
           metadata.append("rdfType", imageMetadata.getRdfType());
           
           //Concern
           ArrayList<Document> concernedItems = new ArrayList<>();
           for (ConcernItem concernItem : imageMetadata.getConcernedItems()) {
               Document concern = new Document();
               concern.append("uri", concernItem.getUri());
               concern.append("rdfType", concernItem.getTypeURI());
               concernedItems.add(concern);
           }
           metadata.append("concern", concernedItems);
           
           //Configuration
           Document configuration = new Document();
           Date date = df.parse(imageMetadata.getConfiguration().getDate());
           configuration.append("date", date);
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
           
           LOGGER.trace("MongoDB insert : " + metadata.toJson());
           imagesCollection.insertOne(metadata);
           createdResourcesUris.add(imageMetadata.getUri());
       }
       
       insertStatus.add(new Status("Resource created", StatusCodeMsg.INFO, "images inserted"));;
       result = new POSTResultsReturn(Boolean.TRUE, Boolean.TRUE, Boolean.TRUE);
       result.setHttpStatus(Response.Status.CREATED);
       result.statusList = insertStatus;
       result.createdResources = createdResourcesUris;
       
       return result;
    }
}

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
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
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
    
    private final MongoCollection<Document> imagesCollection = database.getCollection(PropertiesFileManager.getConfigFileProperty("mongodb_nosql_config", "data"));

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
    
    private BasicDBObject prepareGetNbImagesYearQuery() {                
        BasicDBObject queryUris = new BasicDBObject();
        URINamespaces uriNamespaces = new URINamespaces();
        String imageUriQuery = uriNamespaces.getContextsProperty("pxPlatform") + "/" + Year.now().toString() + "/i/";
        queryUris.append("uri", imageUriQuery.replaceAll("/", "\\/"));
        
        return queryUris;
    }
    
    /**
     * 
     * @return un entier correspondant au nombre d'images dans le système pour cette année.
     */
    public long getNbImagesYear() {
        BasicDBObject query = prepareGetNbImagesYearQuery();
        
        LOGGER.trace(getTraceabilityLogs() + " query : " + query.toString());
        return imagesCollection.count(query);
    }
}

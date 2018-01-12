//**********************************************************************************************
//                                       DatasetDaoMongo.java 
//
// Author(s): Morgane VIDAL
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: September 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  January, 12 2018 update insert data : add 
// possibility to add a dataset with an already existing provenance  
// Subject: A specific Dao to retrieve data on phenotypes. 
//***********************************************************************************************
package phis2ws.service.dao.mongo;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javax.ws.rs.core.Response;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.PropertiesFileManager;
import phis2ws.service.dao.manager.DAOMongo;
import phis2ws.service.dao.phis.AgronomicalObjectDao;
import phis2ws.service.dao.sesame.AgronomicalObjectDaoSesame;
import phis2ws.service.dao.sesame.VariableDaoSesame;
import phis2ws.service.documentation.StatusCodeMsg;
import phis2ws.service.resources.dto.DataDTO;
import phis2ws.service.resources.dto.DatasetDTO;
import phis2ws.service.utils.POSTResultsReturn;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.model.phis.AgronomicalObject;
import phis2ws.service.view.model.phis.Data;
import phis2ws.service.view.model.phis.Dataset;

/**
 * Represents the MongoDB Data Access Object for the datasets
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class DatasetDAOMongo extends DAOMongo<Dataset> {
    
    final static Logger LOGGER = LoggerFactory.getLogger(DatasetDAOMongo.class);
    
    private final MongoCollection<Document> provenanceCollection = database.getCollection(PropertiesFileManager.getConfigFileProperty("mongodb_nosql_config", "provenance"));
    private final MongoCollection<Document> dataCollection = database.getCollection(PropertiesFileManager.getConfigFileProperty("mongodb_nosql_config", "data"));
    
    public String experiment;
    public String variable;
    public ArrayList<String> agronomicalObjects = new ArrayList<>();
    public String startDate;
    public String endDate;
    public String provenance;
    
    public DatasetDAOMongo() {
        super();
    }    
    
    @Override
    protected BasicDBObject prepareSearchQuery() {
        BasicDBObject query = new BasicDBObject();
        
        if (variable != null) {
            query.append("variable", variable);
        }
        
        if (startDate != null && endDate != null) {
            try {
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
                Date start = df.parse(startDate);
                Date end = df.parse(endDate);
                
                query.append("date", BasicDBObjectBuilder.start("$gte", start).add("$lte", end).get());

            } catch (ParseException ex) {
                java.util.logging.Logger.getLogger(DatasetDAOMongo.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        if (agronomicalObjects != null && !agronomicalObjects.isEmpty()) {
            if (agronomicalObjects.size() > 1) {
                BasicDBList or = new BasicDBList();
                for (String agronomicalObject : agronomicalObjects) {
                    BasicDBObject clause = new BasicDBObject("agronomicalObject", agronomicalObject);
                    or.add(clause);
                }
                query.append("$or", or);
            } else {
                query.append("agronomicalObject", agronomicalObjects.get(0));
            }
        }
        
        return query;
    }
    
    /**
     * prepare a provenance search query for mongodb. Search by uri
     * @return 
     */
    protected BasicDBObject prepareSearchProvenance() {
        BasicDBObject query = new BasicDBObject();
        if (provenance != null) {
            query.append("uri", provenance);
        }
        return query;
    }

    /**
     * get experiment's agronomical objects and add them to the searched 
     * agronomical objects list 
     */
    private void updateAgronomicalObjectsWithExperimentsAgronomicalObjects() {
        AgronomicalObjectDaoSesame agronomicalObjectDaoSesame = new AgronomicalObjectDaoSesame();
        agronomicalObjectDaoSesame.experiment = experiment;
        
        ArrayList<AgronomicalObject> agronomicalObjectsSearched = agronomicalObjectDaoSesame.allPaginate();
        
        for (AgronomicalObject agronomicalObject : agronomicalObjectsSearched) {
            this.agronomicalObjects.add(agronomicalObject.getUri());
        }
    }
    
    /**
     * get all datasets corresponding to search params (experiment, agronomical
     * objects, variable, date start, date end)
     * @return datasets list, empty if no search result
     */
    @Override
    public ArrayList<Dataset> allPaginate() {
        //If search by experiment, get experiment's agronomical objects.
        if (experiment != null) {
            updateAgronomicalObjectsWithExperimentsAgronomicalObjects();
        }
        
        BasicDBObject query = prepareSearchQuery();
        
        LOGGER.trace(getTraceabilityLogs() + " query : " + query.toString());
        FindIterable<Document> phenotypesMongo = dataCollection.find(query);

        ArrayList<Dataset> phenotypes = new ArrayList<>();
        Dataset phenotype = new Dataset();
        phenotype.setExperiment(experiment);
        phenotype.setVariableURI(variable);
        
        try (MongoCursor<Document> phenotypesCursor = phenotypesMongo.iterator()) {
            while (phenotypesCursor.hasNext()) {
                Document phenotypeDocument = phenotypesCursor.next();
                
                Data data = new Data();
                data.setAgronomicalObject(phenotypeDocument.getString("agronomicalObject"));
                data.setDate(new SimpleDateFormat("yyyy-MM-dd").format(phenotypeDocument.getDate("date")));
                data.setValue(phenotypeDocument.getString("value"));
                data.setVariable(phenotypeDocument.getString("variable"));
                
                phenotype.addData(data);
            }
        }
        phenotypes.add(phenotype);
        
        return phenotypes;
    }
    
    /**
     * check if a dataset is valid (follows rules).
     * @param datasetDTO
     * @return 
     */
    private boolean isElementValid(DatasetDTO datasetDTO) {
        Map<String, Object> phenotypeOk = datasetDTO.isOk();
        return (boolean) phenotypeOk.get("state");
    }
    
    //SILEX:todo
    //- Separate the check and the insert actions
    //- The check function must be also used in the update
    //\SILEX:todo
    /**
     * check and insert in the mongodb database a list of datasets
     * @param datasetsDTO datasets to insert
     * @return the insertion result
     * @throws Exception 
     */
    private POSTResultsReturn checkAndInsertDatasets(ArrayList<DatasetDTO> datasetsDTO) throws Exception {
        List<Status> insertStatusList = new ArrayList<>();
        List<String> createdProvenances = new ArrayList<>();
        POSTResultsReturn result = null;
        
        ArrayList<Dataset> datasets = new ArrayList<>();
        boolean dataState = true;
        
        
        for (DatasetDTO datasetDTO : datasetsDTO) {
            if (isElementValid(datasetDTO)) {                
                for (DataDTO data : datasetDTO.getData()) {
                    AgronomicalObjectDao agronomicalObjectDao = new AgronomicalObjectDao();
                    if (!agronomicalObjectDao.existInDB(new AgronomicalObject(data.getAgronomicalObject()))) {
                        dataState = false;
                        insertStatusList.add(new Status("Data error", StatusCodeMsg.ERR, "Unknown Agronomical Object URI : " + data.getAgronomicalObject()));
                    }
                }
                
                VariableDaoSesame variableDaoSesame = new VariableDaoSesame();
                if (!variableDaoSesame.existObject(datasetDTO.getVariableUri())) {
                    dataState = false;
                    insertStatusList.add(new Status("Data error", StatusCodeMsg.ERR, "Unknown Variable : " + datasetDTO.getVariableUri()));
                }
                
                Dataset phenotype = datasetDTO.createObjectFromDTO();
                datasets.add(phenotype);
                
            } else {
                dataState = false;
                insertStatusList.add(new Status("Data error", StatusCodeMsg.ERR, "Fields are missing in JSON Data"));
            }
        }
        
        if (dataState) {
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            //SILEX:todo
            //transactions
            //mongodb insertion
            for (Dataset dataset : datasets) {
                //1. provenance creation if needed
                Object provenanceId = null;
                if (dataset.getProvenance().getCreationDate() != null) {
                    Document provenanceDocument = new Document();
                    Date creationDate = df.parse(dataset.getProvenance().getCreationDate());

                    provenanceDocument.append("creationDate", creationDate);
                    provenanceDocument.append("wasGeneratedBy", dataset.getProvenance().getWasGeneratedBy().getWasGeneratedBy());
                    provenanceDocument.append("wasGeneratedByDescription", dataset.getProvenance().getWasGeneratedBy().getWasGeneratedByDescription());
                    provenanceDocument.append("documents", dataset.getProvenance().getDocumentsUris());
                    provenanceDocument.append("uri", dataset.getProvenance().getUri());

                    LOGGER.debug("MongoDB insert : " + provenanceDocument.toJson());

                    provenanceCollection.insertOne(provenanceDocument);
                    provenanceId = provenanceDocument.get("_id");
                } else { //get provenance id
                    provenance = dataset.getProvenance().getUri();
                    BasicDBObject provenanceQuery = prepareSearchProvenance();
                    LOGGER.debug("MongoDB query provenance : " + provenanceQuery.toJson());
                    
                    FindIterable<Document> provenances = provenanceCollection.find(provenanceQuery);
                    try (MongoCursor<Document> provenancesCursor = provenances.iterator()) {
                        while (provenancesCursor.hasNext()) {
                            Document provenanceDocument = provenancesCursor.next();
                            provenanceId = provenanceDocument.get("_id");
                        }
                    }
                }
                createdProvenances.add(dataset.getProvenance().getUri());
                
                ArrayList<Document> dataToInsert = new ArrayList<>();
                //2. Data insertion
                for (Data data : dataset.getData()) {
                    Document d = new Document();
                    Date date = df.parse(data.getDate());
                    
                    d.append("date", date);
                    d.append("variable", dataset.getVariableURI());
                    //SILEX:todo
                    //choose the type of the value with the variable type 
                    //(string or double)
                    d.append("value", Double.parseDouble(data.getValue()));
                    //\SILEX:todo
                    d.append("agronomicalObject", data.getAgronomicalObject());
                    //SILEX:todo
                    //DBRef (https://docs.mongodb.com/manual/reference/database-references/#dbref-explanation)
                    d.append("provenanceId", provenanceId);
                    d.append("provenanceUri", dataset.getProvenance().getUri());
                    //\SILEX:todo
                    
                    LOGGER.debug("MongoDB insert : " + d.toJson());
                   
                    dataToInsert.add(d);
                }
                
                dataCollection.insertMany(dataToInsert);
            }
            
            insertStatusList.add(new Status("Resource created", StatusCodeMsg.INFO, "datasets inserted"));;
            result = new POSTResultsReturn(dataState);
            result.setHttpStatus(Response.Status.CREATED);
            result.createdResources = createdProvenances;
            result.statusList = insertStatusList;
            //\SILEX:todo
        } else {
            result = new POSTResultsReturn(dataState);
            result.setHttpStatus(Response.Status.BAD_REQUEST);
            result.statusList = insertStatusList;
        }
        
        return result;
    }
    
    /**
     * register datasets in MongoDB
     * @param datasetsDTO datasets to save
     * @return insertion result
     */
    public POSTResultsReturn checkAndInsert(ArrayList<DatasetDTO> datasetsDTO) {
        POSTResultsReturn postResult;
        
        try {
            postResult = this.checkAndInsertDatasets(datasetsDTO);
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
            postResult = new POSTResultsReturn(false, Response.Status.INTERNAL_SERVER_ERROR, ex.toString());
        }
        
        return postResult;
    }
}

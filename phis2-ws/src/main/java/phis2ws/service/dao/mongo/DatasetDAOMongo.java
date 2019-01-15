//**********************************************************************************************
//                                       DatasetDaoMongo.java 
//
// Author(s): Morgane Vidal
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: September 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  January, 12 2018 update insert data : add 
// possibility to add a dataset with an already existing provenance  
// Subject: A specific Dao to retrieve data on phenotypes. Here, we choose to
// deals with quantitative varibles with double type. 
//***********************************************************************************************
package phis2ws.service.dao.mongo;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Sorts;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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
import phis2ws.service.dao.manager.DAOMongo;
import phis2ws.service.dao.phis.ScientificObjectDAO;
import phis2ws.service.dao.sesame.ScientificObjectDAOSesame;
import phis2ws.service.dao.sesame.SensorDAOSesame;
import phis2ws.service.dao.sesame.VariableDaoSesame;
import phis2ws.service.documentation.StatusCodeMsg;
import phis2ws.service.resources.dto.DataDTO;
import phis2ws.service.resources.dto.DatasetDTO;
import phis2ws.service.utils.POSTResultsReturn;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.model.phis.ScientificObject;
import phis2ws.service.view.model.phis.Data;
import phis2ws.service.view.model.phis.Dataset;

/**
 * Represents the MongoDB Data Access Object for the datasets
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class DatasetDAOMongo extends DAOMongo<Dataset> {
    
    final static Logger LOGGER = LoggerFactory.getLogger(DatasetDAOMongo.class);
    
    //Mongodb collection of the provenance
    private final MongoCollection<Document> provenanceCollection = database.getCollection(PropertiesFileManager.getConfigFileProperty("mongodb_nosql_config", "provenance"));
    //Mongodb collection of the data
    private final MongoCollection<Document> dataCollection = database.getCollection(PropertiesFileManager.getConfigFileProperty("mongodb_nosql_config", "data"));
    
    //experiment uri concerned by the dataset
    public String experiment;
    //variable uri of the data of the dataset
    public String variable;
    //list of the agronomical objects uris concerned by the dataset
    public ArrayList<String> agronomicalObjects = new ArrayList<>();
    //start date of the data searched (when the user want data in a time interval)
    public String startDate;
    //end date of the data searched (when the user want data in a time interval)
    public String endDate;
    //provenance uri of the dataset
    public String provenance;
    //senesor uri
    public String sensor;
    //incertitude of the data
    public String incertitude;
    
    //Mongodb fields labels 
    //Represents the agronomical object label used for mongodb documents
    private final static String DB_FIELD_AGRONOMICAL_OBJECT = "agronomicalObject";
    //Represents the variable label used for mongodb documents
    private final static String DB_FIELD_VARIABLE = "variable";
    //Represents the date label used for mongodb documents
    private final static String DB_FIELD_DATE = "date";
    //Represents the uri label used for mongodb documents
    private final static String DB_FIELD_URI = "uri";
    //Represents the value label used for mongodb documents. It is the double 
    //value of the data
    private final static String DB_FIELD_VALUE = "value";
    //Represents the creation date label used for mongodb documents.
    private final static String DB_FIELD_CREATION_DATE = "creationDate";
    //Represents the mongodb documents label for the script document uri which 
    //was used to generate the dataset
    private final static String DB_FIELD_WAS_GENERATED_BY = "wasGeneratedBy";
    //Represents the mongodb documents label for the description about the 
    //dataset generation 
    private final static String DB_FIELDS_WAS_GENERATED_BY_DESCRIPTION = "wasGeneratedByDescription";
    //Represents the mongodb documents label for the documents linked to the 
    //dataset
    private final static String DB_FIELDS_DOCUMENTS = "documents";
    //Represents the mongodb documents label for the provenance id
    private final static String DB_FIELDS_PROVENANCE_ID = "provenanceId";
    //Represents the mongodb documents label for the provenance uri
    private final static String DB_FIELDS_PROVENANCE_URI = "provenanceUri";
    //Represents the mongodb documents label for the sensor uri
    private final static String DB_FIELDS_SENSOR = "sensor";
    //Represents the mongodb documents label for the incertitude of data
    private final static String DB_FIELDS_INCERTITUDE = "incertitude";
    
    public DatasetDAOMongo() {
        super();
    }    
    
    /**
     * Search by variable, start date, end date, agronomical object 
     * @see phis2ws.service.dao.manager prepareSearchQuery()
     * @return the search query
     */
    @Override
    protected BasicDBObject prepareSearchQuery() {
        BasicDBObject query = new BasicDBObject();
        
        if (variable != null) {
            query.append(DB_FIELD_VARIABLE, variable);
        }
        
        if (startDate != null && endDate != null) {
            try {
                SimpleDateFormat df = new SimpleDateFormat(DateFormats.YMD_FORMAT);
                Date start = df.parse(startDate);
                Date end = df.parse(endDate);
                
                query.append(DB_FIELD_DATE, BasicDBObjectBuilder.start("$gte", start).add("$lte", end).get());

            } catch (ParseException ex) {
                java.util.logging.Logger.getLogger(DatasetDAOMongo.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        if (agronomicalObjects != null && !agronomicalObjects.isEmpty()) {
            if (agronomicalObjects.size() > 1) {
                BasicDBList or = new BasicDBList();
                for (String agronomicalObject : agronomicalObjects) {
                    BasicDBObject clause = new BasicDBObject(DB_FIELD_AGRONOMICAL_OBJECT, agronomicalObject);
                    or.add(clause);
                }
                query.append("$or", or);
            } else {
                query.append(DB_FIELD_AGRONOMICAL_OBJECT, agronomicalObjects.get(0));
            }
        }
        
        if (sensor != null) {
            query.append(DB_FIELDS_SENSOR, sensor);
        }
        
        if (incertitude != null) {
            query.append(DB_FIELDS_INCERTITUDE, incertitude);
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
            query.append(DB_FIELD_URI, provenance);
        }
        return query;
    }

    /**
     * get experiment's agronomical objects and add them to the searched 
     * agronomical objects list 
     */
    private void updateAgronomicalObjectsWithExperimentsAgronomicalObjects() {
        ScientificObjectDAOSesame agronomicalObjectDaoSesame = new ScientificObjectDAOSesame();
        agronomicalObjectDaoSesame.experiment = experiment;
        
        ArrayList<ScientificObject> agronomicalObjectsSearched = agronomicalObjectDaoSesame.allPaginate();
        
        for (ScientificObject agronomicalObject : agronomicalObjectsSearched) {
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
        FindIterable<Document> datasetMongo = dataCollection
                .find(query)
                .sort(Sorts.ascending(DB_FIELD_DATE));

        ArrayList<Dataset> phenotypes = new ArrayList<>();
        Dataset phenotype = new Dataset();
        phenotype.setExperiment(experiment);
        phenotype.setVariableURI(variable);
        
        try (MongoCursor<Document> datasetCursor = datasetMongo.iterator()) {
            while (datasetCursor.hasNext()) {
                Document datasetDocument = datasetCursor.next();
                
                Data data = new Data();
                data.setAgronomicalObject(datasetDocument.getString(DB_FIELD_AGRONOMICAL_OBJECT));
                data.setDate(new SimpleDateFormat(DateFormats.YMD_FORMAT).format(datasetDocument.getDate(DB_FIELD_DATE)));
                data.setValue(Double.toString(datasetDocument.getDouble(DB_FIELD_VALUE)));
                data.setVariable(datasetDocument.getString(DB_FIELD_VARIABLE));
                if (datasetDocument.getString(DB_FIELDS_SENSOR) != null) {
                    data.setSensor(datasetDocument.getString(DB_FIELDS_SENSOR));
                }
                if (datasetDocument.getString(DB_FIELDS_INCERTITUDE) != null) {
                    data.setIncertitude(datasetDocument.getString(DB_FIELDS_INCERTITUDE));
                }
                
                phenotype.addData(data);
            }
        }
        phenotypes.add(phenotype);
        
        return phenotypes;
    }
    
    //SILEX:todo
    //- Separate the check and the insert actions
    //- The check function must be also used in the update
    //\SILEX:todo
    /**
     * check and insert in the mongodb database a list of datasets.
     * If the provenance creation date is not given, it means that the provenance
     * already exist. The dataset is added to the existing provenance. If the 
     * provenance does not exist and the creation date is given, a new provenance
     * is created
     * @param datasetsDTO datasets to insert
     * @return the insertion result
     * @throws Exception 
     */
    private POSTResultsReturn checkAndInsertDatasets(ArrayList<DatasetDTO> datasetsDTO) throws Exception {
        List<Status> insertStatusList = new ArrayList<>();
        //The uris of the provenances created
        List<String> createdProvenances = new ArrayList<>(); 
        POSTResultsReturn result = null;
        
        ArrayList<Dataset> datasets = new ArrayList<>();
        boolean dataState = true;
        
        //check if data is valid
        for (DatasetDTO datasetDTO : datasetsDTO) {
            //if the datasetDTO follows the rules
            for (DataDTO data : datasetDTO.getData()) {
                //is agronomical object exist ?
                ScientificObjectDAO agronomicalObjectDao = new ScientificObjectDAO();
                if (!agronomicalObjectDao.existInDB(new ScientificObject(data.getAgronomicalObject()))) {
                    dataState = false;
                    insertStatusList.add(new Status(StatusCodeMsg.DATA_ERROR, StatusCodeMsg.ERR, "Unknown Agronomical Object URI : " + data.getAgronomicalObject()));
                }

                //is sensor exist ?
                if (data.getSensor() != null) {
                    SensorDAOSesame sensorDAO = new SensorDAOSesame();
                    if (!sensorDAO.existUri(data.getSensor())) {
                        dataState = false;
                        insertStatusList.add(new Status(StatusCodeMsg.DATA_ERROR, StatusCodeMsg.ERR, "Unknown sensor : " + data.getSensor()));
                    }
                }
            }

            //is variable exist ? 
            VariableDaoSesame variableDaoSesame = new VariableDaoSesame();
            if (!variableDaoSesame.existUri(datasetDTO.getVariableUri())) {
                dataState = false;
                insertStatusList.add(new Status(StatusCodeMsg.DATA_ERROR, StatusCodeMsg.ERR, "Unknown Variable : " + datasetDTO.getVariableUri()));
            }

            Dataset phenotype = datasetDTO.createObjectFromDTO();
            datasets.add(phenotype);
        }
        
        //if data is valid, insert in mongo
        if (dataState) {
            SimpleDateFormat df = new SimpleDateFormat(DateFormats.YMD_FORMAT);
            //SILEX:todo
            //transactions
            //mongodb insertion
            for (Dataset dataset : datasets) {
                //1. provenance creation if needed
                Object provenanceId = null;
                if (dataset.getProvenance().getCreationDate() != null) {
                    //If the provenance creation date is not given, it means that the provenance
                    //already exist. The dataset is added to the existing provenance. If the 
                    //provenance does not exist and the creation date is given, a new provenance
                    //is created
                    Document provenanceDocument = new Document();
                    Date creationDate = df.parse(dataset.getProvenance().getCreationDate());

                    provenanceDocument.append(DB_FIELD_CREATION_DATE, creationDate);
                    provenanceDocument.append(DB_FIELD_WAS_GENERATED_BY, dataset.getProvenance().getWasGeneratedBy().getWasGeneratedBy());
                    provenanceDocument.append(DB_FIELDS_WAS_GENERATED_BY_DESCRIPTION, dataset.getProvenance().getWasGeneratedBy().getWasGeneratedByDescription());
                    provenanceDocument.append(DB_FIELDS_DOCUMENTS, dataset.getProvenance().getDocumentsUris());
                    provenanceDocument.append(DB_FIELD_URI, dataset.getProvenance().getUri());

                    LOGGER.debug("MongoDB insert : " + provenanceDocument.toJson());

                    provenanceCollection.insertOne(provenanceDocument);
                    provenanceId = provenanceDocument.get(DB_FIELD_ID);
                } else { //get provenance id
                    //SILEX:todo
                    //create a Provenance DAO
                    //\SILEX:todo
                    provenance = dataset.getProvenance().getUri();
                    BasicDBObject provenanceQuery = prepareSearchProvenance();
                    LOGGER.debug("MongoDB query provenance : " + provenanceQuery.toJson());
                    
                    FindIterable<Document> provenances = provenanceCollection.find(provenanceQuery);
                    try (MongoCursor<Document> provenancesCursor = provenances.iterator()) {
                        while (provenancesCursor.hasNext()) {
                            Document provenanceDocument = provenancesCursor.next();
                            provenanceId = provenanceDocument.get(DB_FIELD_ID);
                        }
                    }
                }
                createdProvenances.add(dataset.getProvenance().getUri());
                
                ArrayList<Document> dataToInsert = new ArrayList<>();
                //2. Data insertion
                for (Data data : dataset.getData()) {
                    Document d = new Document();
                    Date date = df.parse(data.getDate());
                    
                    d.append(DB_FIELD_DATE, date);
                    d.append(DB_FIELD_VARIABLE, dataset.getVariableURI());
                    //SILEX:todo
                    //choose the type of the value with the variable type 
                    //(string or double)
                    //for the first version, we can only handle double values
                    d.append(DB_FIELD_VALUE, Double.parseDouble(data.getValue()));
                    //\SILEX:todo
                    d.append(DB_FIELD_AGRONOMICAL_OBJECT, data.getAgronomicalObject());
                    //SILEX:todo
                    //DBRef (https://docs.mongodb.com/manual/reference/database-references/#dbref-explanation)
                    d.append(DB_FIELDS_PROVENANCE_ID, provenanceId);
                    d.append(DB_FIELDS_PROVENANCE_URI, dataset.getProvenance().getUri());
                    //\SILEX:todo
                    d.append(DB_FIELDS_SENSOR, data.getSensor());
                    d.append(DB_FIELDS_INCERTITUDE, data.getIncertitude());
                    
                    LOGGER.debug("MongoDB insert : " + d.toJson());
                   
                    dataToInsert.add(d);
                }
                
                dataCollection.insertMany(dataToInsert);
            }
            
            insertStatusList.add(new Status(StatusCodeMsg.RESOURCES_CREATED, StatusCodeMsg.INFO, StatusCodeMsg.DATA_INSERTED));;
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

//******************************************************************************
//                                 DatasetDao.java 
// SILEX-PHIS
// Copyright © INRA 2017
// Creation date: Sept. 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.dao;

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
import opensilex.service.dao.exception.DAODataErrorAggregateException;
import opensilex.service.dao.exception.DAOPersistenceException;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import opensilex.service.PropertiesFileManager;
import opensilex.service.configuration.DateFormats;
import opensilex.service.dao.manager.MongoDAO;
import opensilex.service.documentation.StatusCodeMsg;
import opensilex.service.resource.dto.AgronomicalDataDTO;
import opensilex.service.resource.dto.DatasetDTO;
import opensilex.service.utils.POSTResultsReturn;
import opensilex.service.view.brapi.Status;
import opensilex.service.model.ScientificObject;
import opensilex.service.model.AgronomicalData;
import opensilex.service.model.Dataset;

/**
 * Dataset DAO.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class DatasetDAO extends MongoDAO<Dataset> {
    
    final static Logger LOGGER = LoggerFactory.getLogger(DatasetDAO.class);
    
    /**
     * MongoDB provenance collection 
     */
    private final MongoCollection<Document> provenanceCollection = database.getCollection(PropertiesFileManager.getConfigFileProperty("mongodb_nosql_config", "provenance"));
    
    /**
     * MongoDB data collection
     */
    private final MongoCollection<Document> dataCollection = database.getCollection(PropertiesFileManager.getConfigFileProperty("mongodb_nosql_config", "data"));
    
    /**
     * Experiment URI concerned by the dataset
     */
    public String experiment;
    
    /**
     * Variable URI of the data of the dataset
     */
    public String variable;
    
    /**
     * List of the scientific objects URIs concerned by the dataset
     */
    public ArrayList<String> scientificObjects = new ArrayList<>();
    
    /**
     * Start date of the data searched (when the user want data in a time interval)
     */
    public String startDate;
    
    /**
     * End date of the data searched (when the user want data in a time interval)
     */
    public String endDate;
    
    /**
     * Provenance URI of the dataset
     */
    public String provenance;
    
    /**
     * Sensor URI
     */
    public String sensor;
    
    /**
     * Incertitude of the data
     */
    public String incertitude;
    
    // MongoDB fields labels 
    // Agronomical object label used for mongodb documents
    private final static String DB_FIELD_SCIENTIFIC_OBJECT = "agronomicalObject";
    
    // Variable label used for mongodb documents
    private final static String DB_FIELD_VARIABLE = "variable";
    
    // Date label used for mongodb documents
    private final static String DB_FIELD_DATE = "date";
    
    // URI label used for mongodb documents
    private final static String DB_FIELD_URI = "uri";
    
    // Value label used for mongodb documents. It is the double value of the data
    private final static String DB_FIELD_VALUE = "value";
    
    // Dreation date label used for mongodb documents.
    private final static String DB_FIELD_CREATION_DATE = "creationDate";
    
    // MongoDB documents label for the script document uri which 
    // was used to generate the dataset
    private final static String DB_FIELD_WAS_GENERATED_BY = "wasGeneratedBy";
    
    // MongoDB documents label for the description about the dataset generation 
    private final static String DB_FIELDS_WAS_GENERATED_BY_DESCRIPTION = "wasGeneratedByDescription";
    
    // MongoDB documents label for the documents linked to the dataset
    private final static String DB_FIELDS_DOCUMENTS = "documents";
    
    // MongoDB documents label for the provenance id
    private final static String DB_FIELDS_PROVENANCE_ID = "provenanceId";
    
    // MongoDB documents label for the provenance URI
    private final static String DB_FIELDS_PROVENANCE_URI = "provenanceUri";
    
    // MongoDB documents label for the sensor URI
    private final static String DB_FIELDS_SENSOR = "sensor";
    
    // MongoDB documents label for the incertitude of data
    private final static String DB_FIELDS_INCERTITUDE = "incertitude";  
    
    /**
     * Searches by variable, start date, end date, scientific object.
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
                java.util.logging.Logger.getLogger(DatasetDAO.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        if (scientificObjects != null && !scientificObjects.isEmpty()) {
            if (scientificObjects.size() > 1) {
                BasicDBList or = new BasicDBList();
                for (String agronomicalObject : scientificObjects) {
                    BasicDBObject clause = new BasicDBObject(DB_FIELD_SCIENTIFIC_OBJECT, agronomicalObject);
                    or.add(clause);
                }
                query.append("$or", or);
            } else {
                query.append(DB_FIELD_SCIENTIFIC_OBJECT, scientificObjects.get(0));
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
     * Prepares a provenance search query for MongoDB. Search by URI.
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
     * Gets experiment's scientific objects and add them to the searched
     * scientific objects list.
     */
    private void updateScientificObjectsWithExperimentsScientificObjects() {
        ScientificObjectRdf4jDAO agronomicalObjectDao = new ScientificObjectRdf4jDAO();
        
        ArrayList<ScientificObject> scientificObjectsSearched = agronomicalObjectDao.find(null, null, experiment, null);
        
        scientificObjectsSearched.forEach((scientificObject) -> {
            this.scientificObjects.add(scientificObject.getUri());
        });
    }
    
    /**
     * Gets all the datasets corresponding to search parameters (experiment, 
     * scientific objects, variable, date start, date end).
     * @return datasets list, empty if no search result.
     */
    public ArrayList<Dataset> allPaginate() {
        //If search by experiment, get experiment's scientific objects.
        if (experiment != null) {
            updateScientificObjectsWithExperimentsScientificObjects();
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
                
                AgronomicalData data = new AgronomicalData();
                data.setAgronomicalObject(datasetDocument.getString(DB_FIELD_SCIENTIFIC_OBJECT));
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
     * Checks and inserts in the MongoDB database a list of datasets.
     * If the provenance creation date is not given, it means that the provenance
     * already exist. The dataset is added to the existing provenance. If the 
     * provenance does not exist and the creation date is given, a new provenance
     * is created.
     * @param datasetsDTO datasets to insert
     * @return the insertion result
     * @throws Exception 
     */
    private POSTResultsReturn checkAndInsertDatasets(ArrayList<DatasetDTO> datasetsDTO) throws Exception {
        List<Status> insertStatusList = new ArrayList<>();
        //The uris of the provenances created
        List<String> createdProvenances = new ArrayList<>(); 
        POSTResultsReturn result;
        
        ArrayList<Dataset> datasets = new ArrayList<>();
        boolean dataState = true;
        
        // check if data is valid
        for (DatasetDTO datasetDTO : datasetsDTO) {
            // if the datasetDTO follows the rules
            for (AgronomicalDataDTO data : datasetDTO.getData()) {
                // does the scientific object exist?
                ScientificObjectRdf4jDAO agronomicalObjectDao = new ScientificObjectRdf4jDAO();
                if (!agronomicalObjectDao.existScientificObject(data.getAgronomicalObject())) {
                    dataState = false;
                    insertStatusList.add(new Status(StatusCodeMsg.DATA_ERROR, StatusCodeMsg.ERR, "Unknown Agronomical Object URI : " + data.getAgronomicalObject()));
                }

                // does the sensor exist?
                if (data.getSensor() != null) {
                    SensorDAO sensorDAO = new SensorDAO();
                    if (!sensorDAO.existUri(data.getSensor())) {
                        dataState = false;
                        insertStatusList.add(new Status(StatusCodeMsg.DATA_ERROR, StatusCodeMsg.ERR, "Unknown sensor : " + data.getSensor()));
                    }
                }
            }

            // does the variable exist? 
            VariableDAO variableDao = new VariableDAO();
            if (!variableDao.existUri(datasetDTO.getVariableUri())) {
                dataState = false;
                insertStatusList.add(new Status(StatusCodeMsg.DATA_ERROR, StatusCodeMsg.ERR, "Unknown Variable : " + datasetDTO.getVariableUri()));
            }

            Dataset phenotype = datasetDTO.createObjectFromDTO();
            datasets.add(phenotype);
        }
        
        // if data is valid, insert in Mongo
        if (dataState) {
            SimpleDateFormat df = new SimpleDateFormat(DateFormats.YMD_FORMAT);
            // SILEX:todo
            // transactions
            // MongoDB insertion
            for (Dataset dataset : datasets) {
                //1. provenance creation if needed
                Object provenanceId = null;
                if (dataset.getProvenance().getCreationDate() != null) {
                    // If the provenance creation date is not given, it means that the provenance
                    // already exists. The dataset is added to the existing provenance. If the 
                    // provenance does not exist and the creation date is given, a new provenance
                    // is created
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
                // 2. AgronomicalData insertion
                for (AgronomicalData data : dataset.getData()) {
                    Document d = new Document();
                    Date date = df.parse(data.getDate());
                    
                    d.append(DB_FIELD_DATE, date);
                    d.append(DB_FIELD_VARIABLE, dataset.getVariableURI());
                    //SILEX:todo
                    // choose the type of the value with the variable type 
                    // (string or double)
                    // for the first version, we can only handle double values
                    d.append(DB_FIELD_VALUE, Double.parseDouble(data.getValue()));
                    //\SILEX:todo
                    d.append(DB_FIELD_SCIENTIFIC_OBJECT, data.getAgronomicalObject());
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
            
            insertStatusList.add(new Status(StatusCodeMsg.RESOURCES_CREATED, StatusCodeMsg.INFO, StatusCodeMsg.DATA_INSERTED));
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
     * Registers datasets in MongoDB.
     * @param datasetsDTO datasets to save.
     * @return insertion result.
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

    @Override
    public List<Dataset> create(List<Dataset> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void delete(List<Dataset> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Dataset> update(List<Dataset> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Dataset find(Dataset object) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Dataset findById(String id) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void validate(List<Dataset> objects) throws DAOPersistenceException, DAODataErrorAggregateException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}

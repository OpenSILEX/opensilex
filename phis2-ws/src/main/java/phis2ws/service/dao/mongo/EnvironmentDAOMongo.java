//******************************************************************************
//                           EnvironmentDAOMongo.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 30 oct. 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.dao.mongo;

import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.MongoClient;
import com.mongodb.MongoException;
import com.mongodb.client.ClientSession;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Sorts;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.ws.rs.core.Response;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.configuration.DateFormat;
import phis2ws.service.configuration.DateFormats;
import phis2ws.service.dao.manager.DAOMongo;
import phis2ws.service.dao.sesame.SensorDAOSesame;
import phis2ws.service.dao.sesame.VariableDaoSesame;
import phis2ws.service.documentation.StatusCodeMsg;
import phis2ws.service.utils.POSTResultsReturn;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.model.phis.EnvironmentMeasure;

/**
 * Represents the MongoDB Data Access Object for the environment.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class EnvironmentDAOMongo extends DAOMongo<EnvironmentMeasure> {
    
    private final static Logger LOGGER = LoggerFactory.getLogger(EnvironmentDAOMongo.class);
    
    //MongoFields labels, used to query (CRUD) the environment mongo data
    private final static String DB_FIELD_SENSOR = "sensor";
    private final static String DB_FIELD_VARIABLE = "variable";
    private final static String DB_FIELD_DATE = "date";
    private final static String DB_FIELD_VALUE = "value";
    
    // Variable URI when querying for environment measures (required)
    // e.g. http://www.phenome-fppn.fr/diaphen/id/variable/ev000070
    public String variableUri;
    // End date filter when querying for environment measures (optional)
    // e.g. 2017-06-07 13:14:32+0200
    public String endDate;
    // Start date filter when querying for environment measures (optional)
    // e.g. 2017-06-07 13:14:32+0200
    public String startDate;
    // Sensor URI filter when querying for environment measures (optional)
    // e.g. http://www.phenome-fppn.fr/mauguio/diaphen/2013/sb140227
    public String sensorUri;
    // Determine the sort order by date of the results (optional, true by default)
    public boolean dateSortAsc = true;
    
    /**
     * Get document count according to the prepareSearchQuery
     * @return the document count
     */
    public int count() {
        // Get the collection corresponding to variable uri
        String variableCollection = this.getEnvironmentCollectionFromVariable(variableUri);
        MongoCollection<Document> environmentMeasureVariableCollection = database.getCollection(variableCollection);

        // Get the filter query
        BasicDBObject query = prepareSearchQuery();
        
        // Return the document count
        return (int)environmentMeasureVariableCollection.count(query);
    }

    /**
     * Prepare and return the environment search query with the given parameters
     * @return The environment measure search query
     * @example
     *  {
     *      "date": {
     *          $gte: ISODate("2010-06-15T10:51:00+0200"),
     *          $lt: ISODate("2018-06-15T10:51:00+0200")
     *      },
     *      "variable": "http://www.phenome-fppn.fr/diaphen/id/variable/v0000001",
     *      "sensor": "http://www.phenome-fppn.fr/diaphen/2018/s18001"
     *  }
     */
    @Override
    protected BasicDBObject prepareSearchQuery() {
        BasicDBObject query = new BasicDBObject();
        
        try {
            SimpleDateFormat df = new SimpleDateFormat(DateFormat.YMDTHMSZ.toString());

            // Define date filter depending if start date and/or end date are defined
            if (startDate != null) {
                Date start = df.parse(startDate);

                if (endDate != null) {
                    // In case of start date AND end date defined
                    Date end = df.parse(endDate);
                    query.append(DB_FIELD_DATE, BasicDBObjectBuilder.start("$gte", start).add("$lte", end).get());
                } else {
                    // In case of start date ONLY is defined
                    query.append(DB_FIELD_DATE, BasicDBObjectBuilder.start("$gte", start).get());
                }
            } else if (endDate != null) {
                // In case of end date ONLY is defined
                Date end = df.parse(endDate);
                query.append(DB_FIELD_DATE, BasicDBObjectBuilder.start("$lte", end).get());
            }
        } catch (ParseException ex) {
            LOGGER.error("Invalid date format", ex);
        }
        
        // Add filter if a sensor uri is defined
        if (sensorUri != null) {
            query.append(DB_FIELD_SENSOR, sensorUri);
        }
        
        query.append(DB_FIELD_VARIABLE, variableUri);
        
        LOGGER.debug(getTraceabilityLogs() + " query : " + query.toString());
        
        return query;
    }

    /**
     * Return the paginated list of environment measures corresponding to given parameters
     * which are (see corresponding variable members on this class) :
     * - variableUri
     * - endDate
     * - startDate
     * - sensorUri
     * - dateSortAsc
     * @return List of measures
     */
    public ArrayList<EnvironmentMeasure> allPaginate() {
        // Get the collection corresponding to variable uri
        String variableCollection = this.getEnvironmentCollectionFromVariable(variableUri);
        MongoCollection<Document> environmentMeasureVariableCollection = database.getCollection(variableCollection);

        // Get the filter query
        BasicDBObject query = prepareSearchQuery();
        
        // Get paginated documents
        FindIterable<Document> measuresMongo = environmentMeasureVariableCollection.find(query);
        
        //SILEX:info
        //Measures are always sort by date, either ascending or descending depending on dateSortAsc parameter
        //If dateSortAsc=true, sort by date ascending
        //If dateSortAsc=false, sort by date descending
        //\SILEX:info
        if (dateSortAsc) {
            measuresMongo = measuresMongo.sort(Sorts.ascending(DB_FIELD_DATE));
        } else {
            measuresMongo = measuresMongo.sort(Sorts.descending(DB_FIELD_DATE));
        }
        
        // Define pagination for the request
        measuresMongo = measuresMongo.skip(page * pageSize).limit(pageSize);

        ArrayList<EnvironmentMeasure> measures = new ArrayList<>();
        SimpleDateFormat df = new SimpleDateFormat(DateFormats.YMDHMSZ_FORMAT);
        
        // For each document, create a EnvironmentMeasure Instance and add it to the result list
        try (MongoCursor<Document> measuresCursor = measuresMongo.iterator()) {
            while (measuresCursor.hasNext()) {
                Document measureDocument = measuresCursor.next();
                
                // Create and define the EnvironmentMeasure
                EnvironmentMeasure measure = new EnvironmentMeasure();
                measure.setVariableUri(variableUri);
                measure.setDate(measureDocument.getDate(DB_FIELD_DATE));
                measure.setValue(new BigDecimal(measureDocument.get(DB_FIELD_VALUE).toString()));
                measure.setSensorUri(measureDocument.getString(DB_FIELD_SENSOR));
                
                // Add the measure to the list
                measures.add(measure);
            }
        }
        
        return measures;
    }
    
    /**
     * Check the given list of environment measures.
     * @param environmentMeasures
     * @return the check result with the founded errors
     */
    private POSTResultsReturn check(List<EnvironmentMeasure> environmentMeasures) {
        POSTResultsReturn checkResult = new POSTResultsReturn();
        List<Status> checkStatus = new ArrayList<>();
        
        boolean dataOk = true;
        
        SensorDAOSesame sensorDAO = new SensorDAOSesame();
        VariableDaoSesame variableDAO = new VariableDaoSesame();
        for (EnvironmentMeasure environmentMeasure : environmentMeasures) {
            //1. Check if the sensorUri exist and is a sensor
            if (sensorDAO.existAndIsSensor(environmentMeasure.getSensorUri())) {
                //2. Check if the variableUri exist and is a variable
                if (variableDAO.existAndIsVariable(environmentMeasure.getVariableUri())) {
                    //3. Check if the given sensor measures the given variable. 
                    if (!sensorDAO.isSensorMeasuringVariable(environmentMeasure.getSensorUri(), environmentMeasure.getVariableUri())) {
                        dataOk = false;
                        checkStatus.add(new Status(StatusCodeMsg.WRONG_VALUE, StatusCodeMsg.ERR, 
                            "The given sensor (" + environmentMeasure.getSensorUri() + ") "
                          + "does not measure the given variable (" + environmentMeasure.getVariableUri() + ")."));
                    }
                } else {
                    dataOk = false;
                    checkStatus.add(new Status(StatusCodeMsg.WRONG_VALUE, StatusCodeMsg.ERR, 
                        "Unknwon variable : " + environmentMeasure.getVariableUri()));
                }
            } else {
                dataOk = false;
                    checkStatus.add(new Status(StatusCodeMsg.WRONG_VALUE, StatusCodeMsg.ERR, 
                        "Unknwon sensor : " + environmentMeasure.getSensorUri()));
            }
        }
        
        checkResult = new POSTResultsReturn(dataOk, null, dataOk);
        checkResult.statusList = checkStatus;
        return checkResult;
    }
    
    /**
     * Generates the query to insert a new environment measure in the mongodb database.
     * @param environmentMeasure
     * @example
     * { 
     *      "sensor" : "http://www.phenome-fppn.fr/diaphen/2018/s18521", 
     *      "variable" : "http://www.phenome-fppn.fr/id/variables/v001", 
     *      "value" : 0.5, 
     *      "date" : { "$date" : 1497516660000 } 
     * }
     * @return the document to insert, representing the given environment measure
     * @throws ParseException 
     */
    private Document prepareInsertEnvironmentDocument(EnvironmentMeasure environmentMeasure) {
        Document environmentDocument = new Document();
        
        environmentDocument.append(DB_FIELD_SENSOR, environmentMeasure.getSensorUri());
        environmentDocument.append(DB_FIELD_VARIABLE, environmentMeasure.getVariableUri());
        environmentDocument.append(DB_FIELD_VALUE, environmentMeasure.getValue());
        environmentDocument.append(DB_FIELD_DATE, environmentMeasure.getDate());
        
        LOGGER.debug(environmentDocument.toJson());
        
        return environmentDocument;
    }
    
    /**
     * Get the environment collection name from the given variable. 
     * 
     * @param variableUri
     * @example variableUri http://www.phenome-fppn.fr/id/variables/v001
     * @return the collection name. It corresponds to the last part of the uri.
     * @example collection name : v001
     */
    private String getEnvironmentCollectionFromVariable(String variableUri) {
        String[] split = variableUri.split("/");
        return split[split.length-1];
    }
    
    /**
     * Insert the given envoronment measures in the mongodb database
     * @param environmentMeasures
     * @return the insertion result
     */
    private POSTResultsReturn insert(List<EnvironmentMeasure> environmentMeasures) {
        //SILEX:information
        //We create a collection for each variable. The environment measures are sorted by variable
        //\SILEX:information
        
        // Initialize transaction
        MongoClient client = DAOMongo.getMongoClient();
        ClientSession session = client.startSession();
        session.startTransaction();
        
        POSTResultsReturn result = null;
        List<Status> status = new ArrayList<>();
        List<String> createdResources = new ArrayList<>(); 
        
        HashMap<String, List<Document>> environmentsToInsertByVariable = new HashMap<>();
        
        //1. Prepare all the documents to insert (we will do one insert by variable)
        for (EnvironmentMeasure environmentMeasure : environmentMeasures) {
            Document createEnvironmentMeasure = prepareInsertEnvironmentDocument(environmentMeasure);

            List<Document> environmentsByVariable;
            if (environmentsToInsertByVariable.containsKey(environmentMeasure.getVariableUri())) {
                environmentsByVariable = environmentsToInsertByVariable.get(environmentMeasure.getVariableUri());
            } else {
                environmentsByVariable = new ArrayList<>();
            }

            environmentsByVariable.add(createEnvironmentMeasure);
            environmentsToInsertByVariable.put(environmentMeasure.getVariableUri(), environmentsByVariable);
        }

        //2. Create unique index on sensor/variable/date for each variable collection
        //   Mongo won't create index if it already exists
        Bson indexFields = Indexes.ascending(
            DB_FIELD_DATE,
            DB_FIELD_SENSOR,
            DB_FIELD_VARIABLE
        );
        IndexOptions indexOptions = new IndexOptions().unique(true);
        environmentsToInsertByVariable.keySet().forEach((variableUri) -> {
            database.getCollection(getEnvironmentCollectionFromVariable(variableUri))
                    .createIndex(indexFields, indexOptions);
        });
        
        //3. Insert all the environment measures
        // Use of AtomicBoolean to use it inside the lambda loop (impossible with a standart boolean)
        // @see: https://stackoverflow.com/questions/46713854/which-is-the-best-way-to-set-drop-boolean-flag-inside-lambda-function
        AtomicBoolean hasError = new AtomicBoolean(false);
        environmentsToInsertByVariable.entrySet().forEach((environmentToInsert) -> {
            MongoCollection<Document> environmentMeasureVariableCollection = database.getCollection(getEnvironmentCollectionFromVariable(environmentToInsert.getKey()));

            try {
                environmentMeasureVariableCollection.insertMany(session, environmentToInsert.getValue());
                status.add(new Status(
                    StatusCodeMsg.RESOURCES_CREATED, 
                    StatusCodeMsg.INFO, 
                    StatusCodeMsg.DATA_INSERTED + " for the variable " + environmentToInsert.getKey()
                ));
                createdResources.add(environmentToInsert.getKey());

            } catch (MongoException ex) {
                // Define that an error occurs
                hasError.set(true);
                
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
                        StatusCodeMsg.DATA_REJECTED + " for the measure variable: " + environmentToInsert.getKey() + " - " + ex.getMessage()
                    ));
                }
            }
        });
        
        //4. Prepare result to return
        result = new POSTResultsReturn(hasError.get());
        result.statusList = status;
        
        if (!hasError.get()) {
            // If no errors commit transaction
            session.commitTransaction();
            result.setHttpStatus(Response.Status.CREATED);
            result.createdResources = createdResources;
        } else {
            // If errors abort transaction
            session.abortTransaction();
            result.setHttpStatus(Response.Status.BAD_REQUEST);
        }
        
        // Close transaction session
        session.close();
        return result;
    }
    
    /**
     * Check the given environment measures and insert them if no errors founded.
     * @param environmentMeasures
     * @return the insertion result, with the errors if some have been found.
     */
    public POSTResultsReturn checkAndInsert(List<EnvironmentMeasure> environmentMeasures) {
        POSTResultsReturn checkResult = check(environmentMeasures);
        if (checkResult.getDataState()) {
            return insert(environmentMeasures);
        } else { //Errors in the data
            return checkResult;
        }
    }

    @Override
    public List<EnvironmentMeasure> create(List<EnvironmentMeasure> objects) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void delete(List<EnvironmentMeasure> objects) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<EnvironmentMeasure> update(List<EnvironmentMeasure> objects) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public EnvironmentMeasure find(EnvironmentMeasure object) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public EnvironmentMeasure findById(String id) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}

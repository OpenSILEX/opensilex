//******************************************************************************
//                                       DataDAOMongo.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 1 March 2019
// Contact: vinecnt.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
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
import phis2ws.service.dao.manager.DAOMongo;
import phis2ws.service.dao.sesame.VariableDaoSesame;
import phis2ws.service.documentation.StatusCodeMsg;
import phis2ws.service.ontologies.Oeso;
import phis2ws.service.utils.POSTResultsReturn;
import phis2ws.service.utils.UriGenerator;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.model.phis.Data;

/**
 * Represents the MongoDB Data Access Object.
 */
public class DataDAOMongo extends DAOMongo<Data> {

    private final static Logger LOGGER = LoggerFactory.getLogger(DataDAOMongo.class);

    //MongoFields labels, used to query (CRUD) the mongo data
    private final static String DB_FIELD_URI = "uri";
    private final static String DB_FIELD_OBJECT = "object";
    private final static String DB_FIELD_VARIABLE = "variable";
    private final static String DB_FIELD_PROVENANCE = "provenance";
    private final static String DB_FIELD_DATE = "date";
    private final static String DB_FIELD_VALUE = "value";
    
    public String variableUri;
    public String startDate;
    public String endDate;
    public String objectUri;
    public String provenanceUri;
    public boolean dateSortAsc;
    

    /**
     * Check the given list of data.
     *
     * @param dataList
     * @return the check result with the founded errors
     */
    private POSTResultsReturn check(List<Data> dataList) {
        POSTResultsReturn checkResult = new POSTResultsReturn();
        List<Status> checkStatus = new ArrayList<>();

        boolean dataOk = true;

        VariableDaoSesame variableDAO = new VariableDaoSesame();

        for (Data data : dataList) {
            //2. Check if the variableUri exist and is a variable
            if (!variableDAO.existAndIsVariable(data.getVariableUri())) {
                dataOk = false;
                checkStatus.add(new Status(StatusCodeMsg.WRONG_VALUE, StatusCodeMsg.ERR,
                        "Unknwon variable : " + data.getVariableUri()));

                // TODO check data type depending of variable
            }
        }

        checkResult = new POSTResultsReturn(dataOk, null, dataOk);
        checkResult.statusList = checkStatus;
        return checkResult;
    }

    /**
     * Generates the query to insert a new data in the mongodb
     * database.
     *
     * @param data
     * @return the document to insert, representing the given data
     * @throws ParseException
     */
    private Document prepareInsertDataDocument(Data data) {
        Document document = new Document();

        String key = data.getVariableUri() + data.getObjectUri() + data.getProvenanceUri() + data.getDate();
        try {
            UriGenerator uriGenerator = new UriGenerator();
            String uri = uriGenerator.generateNewInstanceUri(Oeso.CONCEPT_DATA.toString(), null, key);

            while (uriExists(data.getVariableUri(), uri)) {
                uriGenerator.generateNewInstanceUri(Oeso.CONCEPT_DATA.toString(), null, key);
            }
            
            data.setUri(uri);
            
            document.append(DB_FIELD_URI, data.getUri());
            document.append(DB_FIELD_OBJECT, data.getObjectUri());
            document.append(DB_FIELD_VARIABLE, data.getVariableUri());
            document.append(DB_FIELD_DATE, data.getDate());
            document.append(DB_FIELD_PROVENANCE, data.getProvenanceUri());
            document.append(DB_FIELD_VALUE, data.getValue());

            LOGGER.debug(document.toJson());
        } catch (Exception e) {
            LOGGER.error("Exception while generating uri, should never append", e);
        }
        
        return document;
    }

    /**
     * Get the collection name from the given variable.
     *
     * @param variableUri
     * @example variableUri http://www.phenome-fppn.fr/id/variables/v001
     * @return the collection name. It corresponds to the last part of the uri.
     * @example collection name : v001
     */
    private String getCollectionFromVariable(String variableUri) {
        String[] split = variableUri.split("/");
        return split[split.length - 1];
    }

    /**
     * Insert the given data in the mongodb database
     *
     * @param dataList
     * @return the insertion result
     */
    private POSTResultsReturn insert(List<Data> dataList) {
        //SILEX:information
        //We create a collection for each variable. The data are sorted by variable
        //\SILEX:information

        // Initialize transaction
        MongoClient client = DAOMongo.getMongoClient();
        ClientSession session = client.startSession();
        session.startTransaction();

        POSTResultsReturn result = null;
        List<Status> status = new ArrayList<>();
        List<String> createdResources = new ArrayList<>();

        HashMap<String, List<Document>> dataListToInsertByVariable = new HashMap<>();

        //1. Prepare all the documents to insert (we will do one insert by variable)
        for (Data data : dataList) {
            Document createData = prepareInsertDataDocument(data);

            List<Document> dataByVariable;
            if (dataListToInsertByVariable.containsKey(data.getVariableUri())) {
                dataByVariable = dataListToInsertByVariable.get(data.getVariableUri());
            } else {
                dataByVariable = new ArrayList<>();
            }

            createdResources.add(data.getUri());
            
            dataByVariable.add(createData);
            dataListToInsertByVariable.put(data.getVariableUri(), dataByVariable);
        }

        //2. Create unique index on sensor/variable/date for each variable collection
        //   Mongo won't create index if it already exists
        Bson indexFields = Indexes.ascending(
                DB_FIELD_DATE,
                DB_FIELD_OBJECT,
                DB_FIELD_VARIABLE,
                DB_FIELD_PROVENANCE
        );
        IndexOptions indexOptions = new IndexOptions().unique(true);
        dataListToInsertByVariable.keySet().forEach((variableUri) -> {
            database.getCollection(getCollectionFromVariable(variableUri))
                    .createIndex(indexFields, indexOptions);
        });

        //3. Insert all the data
        // Use of AtomicBoolean to use it inside the lambda loop (impossible with a standart boolean)
        // @see: https://stackoverflow.com/questions/46713854/which-is-the-best-way-to-set-drop-boolean-flag-inside-lambda-function
        AtomicBoolean hasError = new AtomicBoolean(false);
        dataListToInsertByVariable.entrySet().forEach((dataToInsert) -> {
            MongoCollection<Document> dataVariableCollection = database.getCollection(getCollectionFromVariable(dataToInsert.getKey()));

            try {
                dataVariableCollection.insertMany(session, dataToInsert.getValue());
                status.add(new Status(
                        StatusCodeMsg.RESOURCES_CREATED,
                        StatusCodeMsg.INFO,
                        StatusCodeMsg.DATA_INSERTED + " for the variable " + dataToInsert.getKey()
                ));

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
                            StatusCodeMsg.DATA_REJECTED + " for the variable: " + dataToInsert.getKey() + " - " + ex.getMessage()
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
     * Check the given data and insert them if no errors founded.
     *
     * @param dataList
     * @return the insertion result, with the errors if some have been found.
     */
    public POSTResultsReturn checkAndInsert(List<Data> dataList) {
        POSTResultsReturn checkResult = check(dataList);
        if (checkResult.getDataState()) {
            return insert(dataList);
        } else { //Errors in the data
            return checkResult;
        }
    }

    @Override
    public ArrayList<Data> allPaginate() {
        // Get the collection corresponding to variable uri
        String variableCollection = this.getCollectionFromVariable(variableUri);
        MongoCollection<Document> dataVariableCollection = database.getCollection(variableCollection);

        // Get the filter query
        BasicDBObject query = prepareSearchQuery();
        
        // Get paginated documents
        FindIterable<Document> dataMongo = dataVariableCollection.find(query);
        
        //SILEX:info
        //Measures are always sort by date, either ascending or descending depending on dateSortAsc parameter
        //If dateSortAsc=true, sort by date ascending
        //If dateSortAsc=false, sort by date descending
        //\SILEX:info
        if (dateSortAsc) {
            dataMongo = dataMongo.sort(Sorts.ascending(DB_FIELD_DATE));
        } else {
            dataMongo = dataMongo.sort(Sorts.descending(DB_FIELD_DATE));
        }
        
        // Define pagination for the request
        dataMongo = dataMongo.skip(page * pageSize).limit(pageSize);

        ArrayList<Data> dataList = new ArrayList<>();
        
        // For each document, create a data Instance and add it to the result list
        try (MongoCursor<Document> measuresCursor = dataMongo.iterator()) {
            while (measuresCursor.hasNext()) {
                Document dataDocument = measuresCursor.next();
                
                // Create and define the data object
                Data data = new Data();
                data.setVariableUri(variableUri);
                data.setUri(dataDocument.getString(DB_FIELD_URI));
                data.setDate(dataDocument.getDate(DB_FIELD_DATE));
                data.setValue(dataDocument.get(DB_FIELD_VALUE));
                data.setObjectUri(dataDocument.getString(DB_FIELD_OBJECT));
                data.setProvenanceUri(dataDocument.getString(DB_FIELD_PROVENANCE));
                
                // Add data to the list
                dataList.add(data);
            }
        }
        
        return dataList;
    }

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
        
        // Add filter if an object uri is defined
        if (objectUri != null) {
            query.append(DB_FIELD_OBJECT, objectUri);
        }

        // Add filter if a provenance uri is defined
        if (provenanceUri != null) {
            query.append(DB_FIELD_PROVENANCE, provenanceUri);
        }
        
        query.append(DB_FIELD_VARIABLE, variableUri);
        
        LOGGER.debug(getTraceabilityLogs() + " query : " + query.toString());
        
        return query;
    }

    public int count() {
                // Get the collection corresponding to variable uri
        String variableCollection = this.getCollectionFromVariable(variableUri);
        MongoCollection<Document> dataVariableCollection = database.getCollection(variableCollection);

        // Get the filter query
        BasicDBObject query = prepareSearchQuery();
        
        // Return the document count
        return (int)dataVariableCollection.countDocuments(query);
    }

    /**
     * Return true if the given URI already exists in variable collection
     * @param variableUri variable which will determine in which collection to look
     * @param uri URI to check
     * @return true if the URI exists and false otherwise
     */
    public boolean uriExists(String variableUri, String uri) {
        String variableCollection = getCollectionFromVariable(variableUri);
        
        BasicDBObject query = new BasicDBObject();
        query.append(DB_FIELD_URI, BasicDBObjectBuilder.start("$exists", true).get());
        
        return database.getCollection(variableCollection).countDocuments(query) > 0; 
    }
}

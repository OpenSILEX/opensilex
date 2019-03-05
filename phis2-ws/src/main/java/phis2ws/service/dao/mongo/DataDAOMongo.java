//******************************************************************************
//                                       DataDAOMongo.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 1 March 2019
// Contact: vinecnt.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.dao.mongo;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoException;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.ws.rs.core.Response;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
     * @example { "sensor" : "http://www.phenome-fppn.fr/diaphen/2018/s18521",
     * "variable" : "http://www.phenome-fppn.fr/id/variables/v001", "value" :
     * 0.5, "date" : { "$date" : 1497516660000 } }
     * @return the document to insert, representing the given data
     * @throws ParseException
     */
    private Document prepareInsertDataDocument(Data data) {
        Document document = new Document();

        String key = data.getObjectUri() + data.getVariableUri() + data.getDate() + data.getProvenanceUri();
        try {
            String uri = new UriGenerator().generateNewInstanceUri(Oeso.CONCEPT_DATA.toString(), null, key);

            document.append(DB_FIELD_URI, uri);
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
                createdResources.add(dataToInsert.getKey());

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
                            StatusCodeMsg.DATA_REJECTED + " for the measure variable: " + dataToInsert.getKey() + " - " + ex.getMessage()
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
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected BasicDBObject prepareSearchQuery() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}

//******************************************************************************
//                                       EnvironmentDAOMongo.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 30 oct. 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.dao.mongo;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import javax.ws.rs.core.Response;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.configuration.DateFormat;
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

    @Override
    protected BasicDBObject prepareSearchQuery() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ArrayList<EnvironmentMeasure> allPaginate() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
    private Document prepareInsertEnvironmentDocument(EnvironmentMeasure environmentMeasure) throws ParseException {
        Document environmentDocument = new Document();
        
        SimpleDateFormat df = new SimpleDateFormat(DateFormat.YMDTHMSZ.toString());
        Date measureDate = df.parse(environmentMeasure.getDate());
        
        environmentDocument.append(DB_FIELD_SENSOR, environmentMeasure.getSensorUri());
        environmentDocument.append(DB_FIELD_VARIABLE, environmentMeasure.getVariableUri());
        environmentDocument.append(DB_FIELD_VALUE, environmentMeasure.getValue());
        environmentDocument.append(DB_FIELD_DATE, measureDate);
        
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
        
        //SILEX:todo
        //Transactions
        //\SILEX:todo
        POSTResultsReturn result = null;
        List<Status> status = new ArrayList<>();
        List<String> createdResources = new ArrayList<>(); 
        
        boolean insert = true;
        
        HashMap<String, List<Document>> environmentsToInsertByVariable = new HashMap<>();
        
        //1. Prepare all the documents to insert (we will do one insert by variable
        for (EnvironmentMeasure environmentMeasure : environmentMeasures) {
            try {
                Document createEnvironmentMeasure = prepareInsertEnvironmentDocument(environmentMeasure);
                
                List<Document> environmentsByVariable;
                if (environmentsToInsertByVariable.containsKey(environmentMeasure.getVariableUri())) {
                    environmentsByVariable = environmentsToInsertByVariable.get(environmentMeasure.getVariableUri());
                } else {
                    environmentsByVariable = new ArrayList<>();
                }
                
                environmentsByVariable.add(createEnvironmentMeasure);
                environmentsToInsertByVariable.put(environmentMeasure.getVariableUri(), environmentsByVariable);
            } catch (ParseException ex) {
                java.util.logging.Logger.getLogger(EnvironmentDAOMongo.class.getName()).log(Level.SEVERE, null, ex);
                status.add(new Status(StatusCodeMsg.ERR, StatusCodeMsg.ERR, ex.toString()));
                insert = false;
            }
        }

        //2. Insert all the environment measures
        if (insert) {
            environmentsToInsertByVariable.entrySet().forEach((environmentToInsert) -> {
                MongoCollection<Document> environmentMeasureVariableCollection = database.getCollection(getEnvironmentCollectionFromVariable(environmentToInsert.getKey()));
                environmentMeasureVariableCollection.insertMany(environmentToInsert.getValue());

                status.add(new Status(StatusCodeMsg.RESOURCES_CREATED, StatusCodeMsg.INFO, StatusCodeMsg.DATA_INSERTED + " for the variable " + environmentToInsert.getKey()));
                createdResources.add(environmentToInsert.getKey());
            });
        }
        
        result = new POSTResultsReturn(insert);
        result.statusList = status;
        if (insert) {
            result.setHttpStatus(Response.Status.CREATED);
            result.createdResources = createdResources;
        } else {
            result.setHttpStatus(Response.Status.INTERNAL_SERVER_ERROR);
        }
        
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
}

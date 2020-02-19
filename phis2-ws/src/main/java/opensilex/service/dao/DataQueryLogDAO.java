//******************************************************************************
//                                  DataQueryLogDAO.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 1 March 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.dao;

import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Sorts;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import opensilex.service.dao.exception.DAODataErrorAggregateException;
import opensilex.service.dao.exception.DAOPersistenceException;
import opensilex.service.dao.exception.ResourceAccessDeniedException;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import opensilex.service.configuration.DateFormat;
import opensilex.service.dao.manager.MongoDAO;
import opensilex.service.model.Data;
import opensilex.service.model.DataQueryLog;

/**
 * Data DAO.
 * @author Arnaud Chaleroy
 */
public class DataQueryLogDAO extends MongoDAO<Data> {

    private final static Logger LOGGER = LoggerFactory.getLogger(DataQueryLogDAO.class);
    
    // MongoDB fields labels, used to useQuery (CRUD) the mongo access logdata 
    private final static String DB_FIELD_USER_IP = "remoteIpAddress";
    private final static String DB_FIELD_USER_URI = "user";
    private final static String DB_FIELD_USER_QUERY = "userQuery";
    private final static String DB_FIELD_QUERY_DATE = "date";
    
    private final static String DB_COLLECTION_QUERY_LOG = "data_access_query_log";

    private final static String QUERY_DATE_VARIABLE = "$date";
    private final static String QUERY_DATE_LABEL = "date";

    private final static String QUERY_MIN_DATE_VARIABLE = "$gte";
    private final static String QUERY_MIN_DATE_LABEL = "gte";

    private final static String QUERY_MAX_DATE_VARIABLE = "$lte";
    private final static String QUERY_MAX_DATE_LABEL = "lte";


            
    public String userUri;
    public String startDate;
    public String endDate;
    public String useQuery;
    public boolean dateSortAsc;

    public DataQueryLogDAO() {
        super();
        this.collection = database.getCollection(DB_COLLECTION_QUERY_LOG);
    }

    /**
     * 
     * @param query useQuery to log
     */
    public void insert(BasicDBObject query){
        Document document = new Document();
        document.append(DB_FIELD_USER_IP, this.remoteUserAdress);
        document.append(DB_FIELD_USER_URI, this.user.getUri());
        String preparedRequest = query.toJson()
                                    .replace(QUERY_DATE_VARIABLE, QUERY_DATE_LABEL)
                                    .replace(QUERY_MIN_DATE_VARIABLE, QUERY_MIN_DATE_LABEL)
                                    .replace(QUERY_MAX_DATE_VARIABLE,  QUERY_MAX_DATE_LABEL);

        document.append(DB_FIELD_USER_QUERY, Document.parse(preparedRequest));
        Date now = new Date(); 
        document.append(DB_FIELD_QUERY_DATE, now);
        this.collection.insertOne(document);
    }
    
    public ArrayList<DataQueryLog> allPaginate() {
        // Get the filter useQuery
        BasicDBObject query = prepareSearchQuery(); 
        
        
        // Get paginated documents
        FindIterable<Document> dataMongo = this.collection.find(query);
        
        //SILEX:info
        //Measures are always sort by date, either ascending or descending depending on dateSortAsc parameter
        //If dateSortAsc=true, sort by date ascending
        //If dateSortAsc=false, sort by date descending
        //\SILEX:info
        if (dateSortAsc) {
            dataMongo = dataMongo.sort(Sorts.ascending(DB_FIELD_QUERY_DATE));
        } else {
            dataMongo = dataMongo.sort(Sorts.descending(DB_FIELD_QUERY_DATE));
        }
        
        // Define pagination for the request
        if (page != null && pageSize != null) {
            dataMongo = dataMongo.skip(page * pageSize).limit(pageSize);
        }

        ArrayList<DataQueryLog> dataAccessLogList = new ArrayList<>();
        
        // For each document, create a AccessLog Instance and add it to the result list
        try (MongoCursor<Document> accessLogCursor = dataMongo.iterator()) {
            while (accessLogCursor.hasNext()) {
                Document dataDocument = accessLogCursor.next();
                
                // Create and define the AccessLog object
                DataQueryLog AccessLog = new DataQueryLog();
                AccessLog.setUserUri(dataDocument.getString(DB_FIELD_USER_URI));
                AccessLog.setDate(dataDocument.getDate(DB_FIELD_QUERY_DATE));
                AccessLog.setQuery(dataDocument.get(DB_FIELD_USER_QUERY, Document.class));
                AccessLog.setRemoteAdress(dataDocument.getString(DB_FIELD_USER_IP));
                
                // Add AccessLog to the list
                dataAccessLogList.add(AccessLog);
            }
        }
        
        return dataAccessLogList;
    }

    /**
     * Prepares and returns the AccessLog search useQuery with the given parameters.
     * @return The AccessLog search useQuery
     * @example
     *  {
     *      "date": {
     *          $gte: ISODate("2010-06-15T10:51:00+0200"),
     *          $lt: ISODate("2018-06-15T10:51:00+0200")
     *      },
     *      "user": "http://www.phenome-fppn.fr/diaphen/id/user/u001",
     *      "remoteIpAddress": "0:0:0:0:0:0:0:1",
     *  }
     */
    protected BasicDBObject prepareSearchQuery(String userUri, String startDate, String endDate, String remoteAddress) {
        BasicDBObject query = new BasicDBObject();
        
        try {
            // Define date filter depending if start date and/or end date are defined
            if (startDate != null) {
                Date start = DateFormat.parseDateOrDateTime(startDate, false);

                if (endDate != null) {
                    // In case of start date AND end date defined
                    Date end = DateFormat.parseDateOrDateTime(endDate, true);
                    query.append(DB_FIELD_QUERY_DATE, BasicDBObjectBuilder.start("$gte", start).add("$lte", end).get());
                } else {
                    // In case of start date ONLY is defined
                    query.append(DB_FIELD_QUERY_DATE, BasicDBObjectBuilder.start("$gte", start).get());
                }
            } else if (endDate != null) {
                // In case of end date ONLY is defined
                Date end = DateFormat.parseDateOrDateTime(endDate, true);
                query.append(DB_FIELD_QUERY_DATE, BasicDBObjectBuilder.start("$lte", end).get());
            }
        } catch (ParseException ex) {
            LOGGER.error("Invalid date format", ex);
        }
        
        // Add filter if an user uri is defined
        if (userUri != null) {
            query.append(DB_FIELD_USER_URI, userUri);
        }
                
        // Add filter if remote address is defined
        if(remoteAddress != null){
            query.append(DB_FIELD_USER_IP, remoteAddress);
        }
        
        LOGGER.debug(getTraceabilityLogs() + " query : " + query.toString());
        
        return query;
    }

    /**
    * Gets AccessLog count according to the prepareSearchQuery.
    * @return the AccessLog count
    */
    public int count(String userUri, String startDate, String endDate, String remoteAddress) {
        // Get the filter useQuery
        BasicDBObject query = prepareSearchQuery(userUri, startDate, endDate, remoteAddress);
        
        // Return the document count
        return (int)this.collection.countDocuments(query);
    }
   
    
    /**
     * Gets AccessLog count according to the prepareSearchQuery.
     * @return the AccessLog count
     */
    public int count() {
        // Get the filter useQuery
        BasicDBObject query = prepareSearchQuery();
        
        // Return the document count
        return (int)this.collection.countDocuments(query);
    }
   
    
    /**
     * Find AccessLog by the given search params.
     * @param page
     * @param pageSize
     * @param userUri
     * @param startDate
     * @param endDate
     * @param remoteAddress
     * @return the AccessLog founded.
     */
    public List<DataQueryLog> find(Integer page, Integer pageSize, String userUri, String startDate, String endDate, String remoteAddress) {
        // Get the collection corresponding to AccessLog access log
        
        // Get the filter useQuery
        BasicDBObject query = prepareSearchQuery(userUri, startDate, endDate, remoteAddress);
        
        // Get paginated documents
        FindIterable<Document> dataMongo = this.collection.find(query);
        
        //SILEX:info
        //Measures are always sort by date, either ascending or descending depending on dateSortAsc parameter
        //If dateSortAsc=true, sort by date ascending
        //If dateSortAsc=false, sort by date descending
        //\SILEX:info
        if (dateSortAsc) {
            dataMongo = dataMongo.sort(Sorts.ascending(DB_FIELD_QUERY_DATE));
        } else {
            dataMongo = dataMongo.sort(Sorts.descending(DB_FIELD_QUERY_DATE));
        }
        
        // Define pagination for the request
        if (page != null && pageSize != null) {
            dataMongo = dataMongo.skip(page * pageSize).limit(pageSize);
        }
        
        ArrayList<DataQueryLog> dataList = new ArrayList<>();
        
        // For each document, create a AccessLog Instance and add it to the result list
        try (MongoCursor<Document> dataAccessLogs = dataMongo.iterator()) {
            while (dataAccessLogs.hasNext()) {
                Document dataDocument = dataAccessLogs.next();
                
                // Create and define the AccessLog object
                DataQueryLog dataAccessLog = new DataQueryLog();
                dataAccessLog.setUserUri(dataDocument.getString(DB_FIELD_USER_URI));
                dataAccessLog.setRemoteAdress(dataDocument.getString(DB_FIELD_USER_IP));
                dataAccessLog.setDate(dataDocument.getDate(DB_FIELD_QUERY_DATE)); 
                dataAccessLog.setQuery(dataDocument.get(DB_FIELD_USER_QUERY, Document.class));
                // Add AccessLog to the list
                dataList.add(dataAccessLog);
            }
        }
        
        return dataList;
    }

    @Override
    public List<Data> create(List<Data> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void delete(List<Data> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Data> update(List<Data> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Data find(Data object) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Data findById(String id) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void validate(List<Data> objects) throws DAOPersistenceException, DAODataErrorAggregateException, DAOPersistenceException, ResourceAccessDeniedException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected BasicDBObject prepareSearchQuery() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}

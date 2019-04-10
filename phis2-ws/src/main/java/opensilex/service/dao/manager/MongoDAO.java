//******************************************************************************
//                                MongoDAO.java 
// SILEX-PHIS
// Copyright © INRA 2016
// Creation date: August 2016
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.dao.manager;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.gridfs.GridFS;
import org.bson.Document;
import org.bson.codecs.configuration.CodecProvider;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import opensilex.service.PropertiesFileManager;
import opensilex.service.configuration.DefaultBrapiPaginationValues;
import opensilex.service.model.User;

/**
 * DAO for MongoDB querying.
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>
 * @param <T>
 */
public abstract class MongoDAO<T> extends DAO<T> {

    /**
     * @see service.properties file
     */
    private final static MongoClient MONGO_CLIENT = new MongoClient(
            new MongoClientURI(PropertiesFileManager.getConfigFileProperty("mongodb_nosql_config", "url")));
    protected GridFS gridFS = new GridFS(MONGO_CLIENT.getDB(PropertiesFileManager.getConfigFileProperty("mongodb_nosql_config", "db")));
    protected MongoDatabase database;
    protected MongoCollection<Document> collection;

    protected Integer page;
    protected Integer pageSize;
    
    //The _id json data key in the mongodb documents
    public final static String DB_FIELD_ID = "_id";
    //The $gte mongo key
    public final static String MONGO_GTE = "$gte";
    //the $lte mongo key
    public final static String MONGO_LTE = "$lte";
    //the $elemMatch mongo key
    public final static String MONGO_ELEM_MATCH = "$elemMatch";
    //the $and mongo key
    public final static String MONGO_AND = "$and";
    //error code send by mongo in case of duplicated data with unique indexes
    public final static int DUPLICATE_KEY_ERROR_CODE = 11000;
    
    /**
     * @see service.properties file
     */
    public MongoDAO() {
        // Add feature to automatically serialize/deserialize class object in mongodb
        // @see http://mongodb.github.io/mongo-java-driver/3.10/bson/pojos/
        CodecProvider pojoCodecProvider = PojoCodecProvider.builder().automatic(true).build();
        CodecRegistry pojoCodecRegistry = fromRegistries(MongoClient.getDefaultCodecRegistry(), fromProviders(pojoCodecProvider));
        
        this.setDatabase(MONGO_CLIENT.getDatabase(PropertiesFileManager.getConfigFileProperty("mongodb_nosql_config", "db")).withCodecRegistry(pojoCodecRegistry));
    }

    public static MongoClient getMongoClient() {
        return MONGO_CLIENT;
    }

    public MongoDatabase getDatabase() {
        return database;
    }

    public void setDatabase(MongoDatabase database) {
        this.database = database;
    }

    public MongoCollection<Document> getCollection() {
        return collection;
    }

    public void setCollection(MongoCollection<Document> collection) {
        this.collection = collection;
    }

    /**
     * @return current page
     */
    public Integer getPage() {
        // The first BrAPI page is 0
        if (page == null || pageSize < 0) {
            return 0;
        }
        return page;
    }

    /**
     * BrAPI page to be used to use pagination in a database query.
     * @return current page + 1
     */
    public Integer getPageForDBQuery() {
        if (page == null || pageSize < 0) {
            return 1;
        }
        return page + 1;
    }

    /**
     * Sets page parameter.
     * @param page
     */
    public void setPage(Integer page) {
        if (page < 0) {
            this.page = Integer.valueOf(DefaultBrapiPaginationValues.PAGE);
        }
        this.page = page;
    }

    /**
     * Gets page size.
     */
    public Integer getPageSize() {
        if (pageSize == null || pageSize < 0) {
            return Integer.valueOf(DefaultBrapiPaginationValues.PAGE_SIZE);
        }
        return pageSize;
    }
    
    /**
     * Sets page size.
     * @param pageSize
     */
    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    /**
     * Prepares a search query.
     * @return BasicDBObject
     */
    abstract protected BasicDBObject prepareSearchQuery();
}

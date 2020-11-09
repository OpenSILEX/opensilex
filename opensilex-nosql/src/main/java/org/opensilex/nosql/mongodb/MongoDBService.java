//******************************************************************************
//                         NoSQLService.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.nosql.mongodb;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.ClientSession;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import static com.mongodb.client.model.Filters.eq;
import com.mongodb.client.model.geojson.codecs.GeoJsonCodecProvider;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.opensilex.OpenSilexModuleNotFoundException;
import org.opensilex.nosql.exceptions.NoSQLAlreadyExistingUriException;
import org.opensilex.nosql.exceptions.NoSQLInvalidURIException;
import org.opensilex.nosql.mongodb.codec.ObjectCodec;
import org.opensilex.nosql.mongodb.codec.URICodec;
import org.opensilex.nosql.mongodb.codec.ZonedDateTimeCodec;
import org.opensilex.service.BaseService;
import org.opensilex.service.ServiceDefaultDefinition;
import org.opensilex.sparql.SPARQLModule;
import org.opensilex.utils.ListWithPagination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceDefaultDefinition(config = MongoDBConfig.class)
public class MongoDBService extends BaseService {
    private final static Logger LOGGER = LoggerFactory.getLogger(MongoDBService.class);
    
    public final String dbName;
    public MongoClient mongoClient;
    public ClientSession session;
    //private final URI baseURI;
    
    public MongoDBService(MongoDBConfig config) throws OpenSilexModuleNotFoundException, URISyntaxException {
        super(config);
        dbName = config.database();
        //baseURI = getOpenSilex().getModuleByClass(SPARQLModule.class).getBaseURI();
    }
    
    @Override
    public void startup() {
        mongoClient = getMongoDBClient();
        session = mongoClient.startSession();
    }
    
    @Override
    public void shutdown() {
        mongoClient.close();
    }
    
    public URI getBaseURI() throws OpenSilexModuleNotFoundException {
        return getOpenSilex().getModuleByClass(SPARQLModule.class).getBaseURI();
    }
    
    public MongoDBConfig getImplementedConfig() {
        return (MongoDBConfig) this.getConfig();
    }
    
    private int transactionLevel = 0;
    public void startTransaction() {
        if (transactionLevel == 0) {
            LOGGER.debug("MONGO TRANSACTION START");
            session.startTransaction();
        }
        transactionLevel++;
    }

    public void commitTransaction() {
        transactionLevel--;
        if (transactionLevel == 0) {
            LOGGER.debug("MONGO TRANSACTION COMMIT");
            session.commitTransaction();
        }
    }

    public void rollbackTransaction() throws Exception {
        if (transactionLevel != 0) {
            LOGGER.error("MONGO TRANSACTION ROLLBACK");
            transactionLevel = 0;
            session.abortTransaction();
        }
    }
    
    
    public <T extends MongoModel> void create(T instance, Class<T> instanceClass, String collectionName, String prefix) throws Exception {   
        if (instance.getUri() == null){
                generateUniqueUriIfNullOrValidateCurrent(instance, prefix, collectionName);
        }        
        MongoCollection<T> collection = mongoClient.getDatabase(dbName).getCollection(collectionName, instanceClass);
        collection.insertOne(instance);    
    }
    
    public <T extends MongoModel> void createAll(List<T> instances, Class<T> instanceClass, String collectionName, String prefix) throws Exception {   
        
        for (T instance:instances) {
            if (instance.getUri() == null){
                generateUniqueUriIfNullOrValidateCurrent(instance, prefix, collectionName);
            }
        }
        
        session.startTransaction();
        MongoCollection<T> collection = mongoClient.getDatabase(dbName).getCollection(collectionName, instanceClass);
            
        try {
            collection.insertMany(session, instances);
            session.commitTransaction();
        } catch (Exception exception) {
            session.abortTransaction();
            throw exception;
        } finally {
            session.close();
        }
    
    }
    
    public <T> T findByURI(Class<T> instanceClass, String collectionName, URI uri) throws NoSQLInvalidURIException {
        MongoCollection<T> collection = mongoClient.getDatabase(dbName).getCollection(collectionName, instanceClass);
        T instance = (T) collection.find(eq("uri", uri)).first();
        if (instance == null) {
            throw new NoSQLInvalidURIException(uri);
        } else {
            return instance;
        }            
        
    }
    
    public <T> boolean uriExists(Class<T> instanceClass, String collectionName, URI uri) {
        try {
            T instance = findByURI(instanceClass, collectionName, uri);
            return true;
        } catch (NoSQLInvalidURIException ex) {
            return false;
        } 
    }
    
    public <T> ListWithPagination<T> searchWithPagination(
            Class<T> instanceClass, 
            String collectionName, 
            Document filter, 
            Integer page,
            Integer pageSize) {
        List<T> results = new ArrayList<T>();
        MongoCollection<T> collection = mongoClient.getDatabase(dbName).getCollection(collectionName, instanceClass);
        long resultsNumber = collection.countDocuments(filter);
        int total = (int) resultsNumber;

        if (total > 0) {
            FindIterable<T> queryResult = collection.find(filter).skip(page * pageSize).limit(pageSize);

            for (T res:queryResult) {
                results.add(res);
            }
        }      

        return new ListWithPagination(results, page, pageSize, total);
                
    }
    
    public <T extends MongoModel> void delete(Class<T> instanceClass, String collectionName, URI uri) throws NoSQLInvalidURIException  {
        MongoCollection<T> collection = mongoClient.getDatabase(dbName).getCollection(collectionName, instanceClass);
        T instance = findByURI(instanceClass, collectionName, uri); 
        if (instance == null) {
            throw new NoSQLInvalidURIException(uri);
        }
        collection.deleteOne(eq("uri", uri));     
    }
    
    public <T extends MongoModel> void update(T newInstance, Class<T> instanceClass, String collectionName) throws NoSQLInvalidURIException {
        MongoCollection<T> collection = mongoClient.getDatabase(dbName).getCollection(collectionName, instanceClass);
        T instance = findByURI(instanceClass, collectionName, newInstance.getUri()); 
        if (instance == null) {
            throw new NoSQLInvalidURIException(newInstance.getUri());
        }
        collection.findOneAndReplace(eq("uri", newInstance.getUri()), newInstance);
    }

    public final MongoClient getMongoDBClient() {
        String connectionString = "mongodb://";
        MongoDBConfig cfg = getImplementedConfig();
        if (cfg.username() != null && cfg.password() != null && !cfg.username().isEmpty() && !cfg.password().isEmpty()) {
            connectionString += cfg.username() + ":" + cfg.password() + "@";
        }
        connectionString += cfg.host() + ":" + cfg.port();
        connectionString += "/" + cfg.database();

        Map<String, String> options = new HashMap<>();
        options.putAll(cfg.options());
        if (cfg.authDB() != null) {
            options.put("authSource", cfg.authDB());
        }
        if (options.size() > 0) {
            connectionString += "?";
            Set<String> keys = options.keySet();
            int i = 0;
            for (String key : keys) {
                String value = options.get(key);
                connectionString += key + "=" + value;
                i++;

                if (i < options.size()) {
                    connectionString += "&";
                }
            }
        }

        CodecRegistry codecRegistry = CodecRegistries.fromRegistries(
                MongoClientSettings.getDefaultCodecRegistry(),
                CodecRegistries.fromCodecs(new URICodec(), new ZonedDateTimeCodec(), new ObjectCodec()),
                CodecRegistries.fromProviders(
                        new GeoJsonCodecProvider(),
                        PojoCodecProvider.builder().register(URI.class).automatic(true).build()
                )
        );

        MongoClientSettings clientSettings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(connectionString))
                .codecRegistry(codecRegistry)
                .build();
        return MongoClients.create(clientSettings);
    }
    
    /**
     * Method to generate a valid URI for the instance
     *
     * @param <T>
     * @param instance will be updated by a new generated URI
     * @param prefix
     * @param collectionName
     * @throws Exception
     */
    public <T extends MongoModel> void generateUniqueUriIfNullOrValidateCurrent(T instance, String prefix, String collectionName) throws Exception {
        URI uri = instance.getUri();

        if (uri == null) {

            int retry = 0;
            String graphPrefix = getBaseURI().resolve(prefix).toString();
            uri = instance.generateURI(graphPrefix, instance, retry);            
            while (uriExists(instance.getClass(), collectionName, uri)) {
                uri = instance.generateURI(graphPrefix, instance, retry++);
            }
            instance.setUri(uri);
            
       } else if (uriExists(instance.getClass(), collectionName, uri)) {
            throw new NoSQLAlreadyExistingUriException(uri);
        }
    }
    
//    public <T extends MongoModel> void generateUniqueUriIfNullOrValidateCurrent(T instance, String prefix, String collectionName) throws Exception {
//        generateUniqueUriIfNullOrValidateCurrent(instance, prefix, collectionName);
//    }
    
}

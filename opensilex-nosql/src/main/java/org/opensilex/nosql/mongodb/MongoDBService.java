//******************************************************************************
//                         NoSQLService.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.nosql.mongodb;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.geojson.codecs.GeoJsonCodecProvider;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.opensilex.nosql.mongodb.codec.URICodec;
import org.opensilex.service.BaseService;
import org.opensilex.service.ServiceDefaultDefinition;
import org.opensilex.utils.ListWithPagination;

@ServiceDefaultDefinition(config = MongoDBConfig.class)
public class MongoDBService extends BaseService {

    private final MongoDatabase db;
    
    public MongoDBService(MongoDBConfig config) {
        super(config);
        this.db = getMongoDBClient().getDatabase(config.database());
    }
    
    public MongoDBConfig getImplementedConfig() {
        return (MongoDBConfig) this.getConfig();
    }
    
    public <T> T create(T instance, Class<T> instanceClass, String collectionName) {       
        MongoCollection<T> collection = this.db.getCollection(collectionName, instanceClass);
        collection.insertOne(instance);
        return instance;
    }
    
    public <T> T findByURI(Class<T> instanceClass, String collectionName, URI uri)  {
        Document filter = new Document("uri", uri);
        MongoCollection collection = this.db.getCollection(collectionName, instanceClass);
        return (T) collection.find(filter).first();
    }
    
    public <T> ListWithPagination<T> searchWithPagination(
            Class<T> instanceClass, 
            String collectionName, 
            Document filter, 
            Integer page,
            Integer pageSize) {
        List<T> results = new ArrayList<T>();
        MongoCollection collection = this.db.getCollection(collectionName, instanceClass);
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
//
//    public void delete(Class cls, Object key) throws NamingException;
//
//    public <T> T findById(Class cls, Object key) throws NamingException;
//
//    public Long count(JDOQLTypedQuery query) throws NamingException;
//
//    public Object update(Object instance) throws NamingException;
//
//    public void createAll(Collection<Object> instances) throws NamingException;
//
//    public void deleteAll(Collection<Object> instances) throws NamingException;
//
//    public Long deleteAll(JDOQLTypedQuery query) throws NamingException;

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
                CodecRegistries.fromCodecs(new URICodec()),
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
    
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.nosql;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.opensilex.OpenSilex;
import org.opensilex.OpenSilexModule;
import org.opensilex.module.ModuleConfig;
import org.opensilex.nosql.mongodb.MongoDBConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author vidalmor
 */
public class NoSQLModule extends OpenSilexModule {

    private final static Logger LOGGER = LoggerFactory.getLogger(NoSQLModule.class);
    
    @Override
    public Class<? extends ModuleConfig> getConfigClass() {
        return NoSQLConfig.class;
    }

    @Override
    public String getConfigId() {
        return "big-data";
    }

    @Override
    public void install(boolean reset) throws Exception {
        initMongo(reset);
    }

    @Override
    public void check() throws Exception {
        MongoDBConfig config = OpenSilex.getInstance().loadConfigPath("big-data.nosql.mongodb", MongoDBConfig.class);
        MongoClient mongo = getMongoClient(config);
        MongoDatabase db = mongo.getDatabase(config.database());
        
        MongoCollection<Document> c = db.getCollection("test");
        
        c.insertOne(new Document("test", "1234"));
        
        long result = c.countDocuments(new BasicDBObject("test", "1234"));
        
        if (result != 1) {
            LOGGER.error("There is a problem in your mongo configuration");
            throw new Exception("There is a problem in your mongo configuration");
        }
        
        c.drop();
        
        result = c.countDocuments(new BasicDBObject("test", "1234"));
        
        if (result != 0) {
            LOGGER.error("There is a problem in your mongo configuration");
            throw new Exception("There is a problem in your mongo configuration");
        }
    }

    private static MongoClient getMongoClient(MongoDBConfig config) {
        String host = config.host();
        int port = config.port();
        String user = config.username();
        String password = config.password();
        String authdb = config.authDB();
        String url = "mongodb://";

        if (!user.equals("") && !password.equals("")) {
            url += user + ":" + password + "@";
        }

        url += host + ":" + port + "/";

        if (!authdb.equals("")) {
            url += "?authSource=" + authdb;
        }

        MongoClient mongo = new MongoClient(new MongoClientURI(url));

        return mongo;
    }

    public static void initMongo(boolean reset) throws Exception {
        MongoDBConfig config = OpenSilex.getInstance().loadConfigPath("big-data.nosql.mongodb", MongoDBConfig.class);
        MongoClient mongo = getMongoClient(config);

        MongoDatabase adminDb = mongo.getDatabase("admin");
        if (!reset) {
            adminDb.runCommand(new Document("replSetInitiate", new Document()));
        } else {
            mongo.dropDatabase(config.database());
        }
    }

}

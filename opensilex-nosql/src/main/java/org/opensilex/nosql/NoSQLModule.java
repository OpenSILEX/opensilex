/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.nosql;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.opensilex.OpenSilexModule;
import org.opensilex.nosql.mongodb.MongoDBConfig;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author vidalmor
 */
public class NoSQLModule extends OpenSilexModule {

    private static final Logger LOGGER = LoggerFactory.getLogger(NoSQLModule.class);

    @Override
    public Class<?> getConfigClass() {
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

    /**
     * @see http://www.datanucleus.org/products/accessplatform/jdo/enhancer.html#api
     * @see https://github.com/datanucleus/tests/blob/master/jdo/general/src/test/org/datanucleus/tests/DynamicEnhanceSchemaToolTest.java
     * @throws Exception
     */
    @Override
    public void setup() throws Exception {
    }

    @Override
    public void check() throws Exception {
        MongoDBConfig config = getOpenSilex().loadConfigPath(MongoDBConfig.DEFAULT_CONFIG_PATH, MongoDBConfig.class);
        MongoClient mongo = MongoDBService.buildMongoDBClient(config);
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

        mongo.close();
    }
    
    public void initMongo(boolean reset) throws Exception {
        MongoDBConfig config = getOpenSilex().loadConfigPath(MongoDBConfig.DEFAULT_CONFIG_PATH, MongoDBConfig.class);

        try(MongoClient mongo = MongoDBService.buildMongoDBClient(config)){

            LOGGER.info("Install with parameters : Reset :" + reset + ", Db : " + config.database());
            if (reset) {
                MongoDatabase db = mongo.getDatabase(config.database());
                db.drop();
            } else {
                try {
                    MongoDatabase adminDb = mongo.getDatabase("admin");
                    Document runCommand = adminDb.runCommand(new Document("replSetGetStatus", 1));
                    LOGGER.info("Replica set information : " + runCommand.toJson());
                } catch (Exception e) {
                    LOGGER.error("No configured replica set.");
                    LOGGER.error("More information at https://github.com/OpenSILEX/opensilex/blob/master/opensilex-doc/src/main/resources/databases/mongodb.md");
                    throw e;
                }
            }
        }

    }
    
}

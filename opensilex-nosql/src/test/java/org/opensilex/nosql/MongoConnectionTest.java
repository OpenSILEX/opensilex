/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.nosql;

import javax.jdo.PersistenceManager;
import javax.naming.NamingException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensilex.nosql.model.TestMongoDocument;
import org.opensilex.nosql.mongodb.MongoDBConfig;
import org.opensilex.nosql.mongodb.MongoDBConnection;

/**
 *
 * @author charlero
 */
public class MongoConnectionTest extends NoSQLServiceTest {

    @BeforeClass
    public static void setupMongo() throws Exception {
        NoSQLServiceTest.initialize();
//        MongoDBConnection mongoDBConnection = opensilex.getServiceInstance("mongodb", MongoDBConnection.class);
        MongoDBConfig config = opensilex.loadConfigPath("big-data.nosql.mongodb", MongoDBConfig.class);
        System.out.println("org.opensilex.nosql.MongoConnectionTest.setupMongo()");
        System.out.println(connection.getOpenSilex().getDefaultLanguage());
        connection = new MongoDBConnection(config);
        connection.setOpenSilex(opensilex);
        connection.setup();
        connection.startup();
//        
//        service = new NoSQLService( new MongoDBConnection(config));
//        service.setOpenSilex(opensilex);
//        service.setup();
//        service.startup();
//        NoSQLServiceTest.initialize();

    }

    @AfterClass
    public static void cleanMongo() throws Exception {
        connection.shutdown();
    }

    @Test
    public void createTest() throws NamingException {
        try ( // TODO implement mongodb startup mechanism
                PersistenceManager persistenceManager = connection.getPersistenceManager()) {
            persistenceManager.makePersistent(new TestMongoDocument("test", 1));
        }

    }
}

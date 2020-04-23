/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.nosql;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.naming.NamingException;
import org.junit.AfterClass;
import org.junit.Assert;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.opensilex.nosql.NoSQLServiceTest.connection;
import static org.opensilex.nosql.NoSQLServiceTest.initialize;
import org.opensilex.nosql.model.TestMongoDocument;
import org.opensilex.nosql.mongodb.MongoDBConfig;
import org.opensilex.nosql.mongodb.MongoDBConnection;

/**
 *
 * @author charlero
 */
public class EmbedMongoTest extends NoSQLServiceTest {

    @BeforeClass
    public static void setupEmbedMongo() throws Exception {
        System.out.println("org.opensilex.nosql.MongoConnectionTest.setupMongo()");

        MongoDBConfig embedMongo = new MongoDBConfig() {
            @Override
            public String host() {
                return "127.0.0.1";
            }

            @Override
            public int port() {
                return 37017;
            }

            @Override
            public String database() {
                return "test";
            }

            @Override
            public String username() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public String password() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public String authDB() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public Map<String, String> options() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        };
        connection = new MongoDBConnection(embedMongo);
        connection.setOpenSilex(opensilex);
        connection.setup();
        connection.startup();
        initialize();
        Assert.assertTrue(connection.getPersistenceManager() != null);

    }

    @AfterClass
    public static void cleanEmbedMongo() throws Exception {
        connection.shutdown();
    }

    @Test
    public void createEmbedMongoTest() throws NamingException {
        String docTestname = "test" + UUID.randomUUID().toString();
        try ( // TODO implement mongodb startup mechanism

            PersistenceManager persistenceManager = connection.getPersistenceManager()) {
            persistenceManager.makePersistent(new TestMongoDocument(docTestname, 1));
            Query q = persistenceManager.newQuery(TestMongoDocument.class);
            q.setFilter("name == '"+docTestname+ "'"); 
            List<TestMongoDocument> results = q.executeList();
            assertTrue(!results.isEmpty());
//            JDOQLTypedQuery<TestMongoDocument> tq = connection.getPersistenceManager().newJDOQLTypedQuery(TestMongoDocument.class);
//            QTestMongoDocument cand = QTestMongoDocument.candidate();
//            List<TestMongoDocument> results = tq.filter(cand.name.eq(name).and(cand.value.eq(1)))
//                    .executeList();
//            assertTrue(!results.isEmpty());
        }

    }
}

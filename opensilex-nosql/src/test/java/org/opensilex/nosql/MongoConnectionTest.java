/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.nosql;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import javax.jdo.JDOQLTypedQuery;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.naming.NamingException;


import org.junit.AfterClass;
import org.junit.Assert;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.opensilex.integration.test.IntegrationTestCategory;
import org.opensilex.nosql.model.TestMongoDocument;
import org.opensilex.nosql.mongodb.MongoDBConnection;

/**
 *
 * @author charlero
 */
@Category(IntegrationTestCategory.class)
public class MongoConnectionTest extends NoSQLServiceTest {

    @BeforeClass
    public static void setupMongo() throws Exception {
        System.out.println("org.opensilex.nosql.MongoConnectionTest.setupMongo()");
        connection = new MongoDBConnection();
        connection.setOpenSilex(opensilex);
        connection.setup();
        connection.startup();
        initialize();
        Assert.assertTrue(connection.getPersistenceManager() != null);

    }
    

    @AfterClass
    public static void cleanMongo() throws Exception {
         try (PersistenceManager persistenceManager = connection.getPersistenceManager()) {
            JDOQLTypedQuery<TestMongoDocument> tq = persistenceManager.newJDOQLTypedQuery(TestMongoDocument.class);
            tq.deletePersistentAll();
         }
        connection.shutdown();
    }

    @Test
    public void createTest() throws NamingException, IOException {
        String docTestname = "test" + UUID.randomUUID().toString();
        try (PersistenceManager persistenceManager = connection.getPersistenceManager()) {
            persistenceManager.makePersistent(new TestMongoDocument(docTestname, 1));
            Query q = persistenceManager.newQuery(TestMongoDocument.class);
            q.setFilter("name == '" + docTestname + "'");
            List<TestMongoDocument> results = q.executeList();
            assertTrue(!results.isEmpty());
//            try (JDOQLTypedQuery<TestMongoDocument> tq = persistenceManager.newJDOQLTypedQuery(TestMongoDocument.class)) {
//                QTestMongoDocument cand = QTestMongoDocument.candidate();
//                List<TestMongoDocument> results = tq.filter(cand.name.eq(name).and(cand.value.eq(1)))
//                        .executeList();
//                assertTrue(!results.isEmpty());
//            }
         }

    }
    
}

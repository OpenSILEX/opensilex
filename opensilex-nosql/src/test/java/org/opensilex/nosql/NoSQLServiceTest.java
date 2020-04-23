//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
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
import org.junit.Test;
import org.opensilex.nosql.model.TestDocument;
import org.opensilex.nosql.service.NoSQLConnection;
import org.opensilex.nosql.service.NoSQLService;
import org.opensilex.unit.test.AbstractUnitTest;

/**
 *
 * @author vincent
 */
public abstract class NoSQLServiceTest extends AbstractUnitTest {

    protected static NoSQLService service;

    public static void initialize(NoSQLConnection connection) throws Exception {
        service = new NoSQLService(connection);
        Assert.assertTrue(service.getPersistenceManager() != null);
    }

    @AfterClass
    public static void destroy() throws Exception {
        try (PersistenceManager persistenceManager = service.getPersistenceManager()) {
            JDOQLTypedQuery<TestDocument> tq = persistenceManager.newJDOQLTypedQuery(TestDocument.class);
            tq.deletePersistentAll();
        }

        service.shutdown();
    }

    @Test
    public void createTest() throws NamingException, IOException {
        String docTestname = "test" + UUID.randomUUID().toString();
        try (PersistenceManager persistenceManager = service.getPersistenceManager()) {
            persistenceManager.makePersistent(new TestDocument(docTestname, 1));
            Query q = persistenceManager.newQuery(TestDocument.class);
            q.setFilter("name == '" + docTestname + "'");
            List<TestDocument> results = q.executeList();
            assertTrue(!results.isEmpty());
//            try (JDOQLTypedQuery<TestMongoDocument> tq = persistenceManager.newJDOQLTypedQuery(TestDocument.class)) {
//                QTestMongoDocument cand = QTestMongoDocument.candidate();
//                List<TestMongoDocument> results = tq.filter(cand.name.eq(name).and(cand.value.eq(1)))
//                        .executeList();
//                assertTrue(!results.isEmpty());
//            }
        }

    }

}

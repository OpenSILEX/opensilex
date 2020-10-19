//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.nosql;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Random;
import javax.jdo.JDOQLTypedQuery;
import javax.jdo.PersistenceManager;
import javax.naming.NamingException;
import org.junit.AfterClass;
import org.junit.Assert;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.opensilex.integration.test.AbstractIntegrationTest;
import org.opensilex.nosql.model.QTestModel;
import org.opensilex.nosql.model.TestModel;
import org.opensilex.nosql.service.NoSQLService;

/**
 *
 * @author vincent
 */
public abstract class DataNucleusServiceTest extends AbstractIntegrationTest {

    protected static NoSQLService service;

    protected TestModel createModel() {
        Date date = new Date();
        Timestamp timestamp = new Timestamp(date.getTime());

        String docTestname = "test-" + timestamp.toString();
        return new TestModel(docTestname, getRandomIntegerBetweenRange(5, 10));
    }

    protected static int getRandomIntegerBetweenRange(int min, int max) {
        Random r = new Random();
        return r.nextInt((max - min) + 1) + min;
    }

    public static void initialize(NoSQLService service) throws Exception {
        service.setOpenSilex(opensilex);
        service.setup();
        service.startup();
        DataNucleusServiceTest.service = service;
        Assert.assertTrue(service.getPersistentConnectionManager() != null);
    }

    @AfterClass
    public static void destroy() throws Exception {
        try (PersistenceManager persistenceManager = service.getPersistentConnectionManager()) {
            JDOQLTypedQuery<TestModel> tq = persistenceManager.newJDOQLTypedQuery(TestModel.class);
            tq.deletePersistentAll();
        }

        service.shutdown();
    }

    @Test
    public void createTest() throws NamingException, IOException {
        int size = 0;

        TestModel testModel = createModel();
        service.create(testModel);

        try (PersistenceManager persistenceManager = service.getPersistentConnectionManager()) {
            try (JDOQLTypedQuery<TestModel> tq = persistenceManager.newJDOQLTypedQuery(TestModel.class)) {
                QTestModel cand = QTestModel.candidate();
                List<TestModel> results = tq.filter(cand.name.eq(testModel.getName()))
                        .executeList();
                size = results.size();
            }
        }
        assertTrue(size == 1);

        TestModel modelFind = service.findById(TestModel.class, testModel.getName());
        assertTrue(modelFind.getName().equals(testModel.getName()));
    }

    @Test
    public void findTest() throws NamingException, IOException {
        int size = 0;
        TestModel testModel = createModel();

        service.create(testModel);
        service.create(createModel());

        TestModel modelFind = service.findById(TestModel.class, testModel.getName());
        assertTrue(modelFind != null);
        assertTrue(modelFind.getName().equals(testModel.getName()));

        TestModel modelFind2 = service.findById(TestModel.class, testModel.getName() + "test2");
        assertNull(modelFind2);

        try (PersistenceManager persistenceManager = service.getPersistentConnectionManager()) {
            try (JDOQLTypedQuery<TestModel> tq = persistenceManager.newJDOQLTypedQuery(TestModel.class)) {
                QTestModel cand = QTestModel.candidate();
                List<TestModel> results = tq.executeList();
                size = results.size();
            }
        }
        assertTrue(size == 2);

    }

//    @Test
    public void deleteObjectTest() throws NamingException, IOException {
        TestModel testModel = createModel();
        service.create(testModel);
        service.create(createModel());
        service.create(createModel());

        TestModel modelFind = service.findById(TestModel.class, testModel.getName());
        assertTrue(modelFind != null);

        service.delete(TestModel.class, testModel.getName());
        TestModel modelFind2 = service.findById(TestModel.class, testModel.getName());
        assertTrue(modelFind2 != null);

    }
}

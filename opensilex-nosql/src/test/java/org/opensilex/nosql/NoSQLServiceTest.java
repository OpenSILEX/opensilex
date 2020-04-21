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
import org.opensilex.nosql.service.NoSQLConnection;
import org.opensilex.unit.test.AbstractUnitTest;

/**
 *
 * @author vincent
 */
public abstract class NoSQLServiceTest extends AbstractUnitTest {

    protected static NoSQLModule module;

    protected static NoSQLConnection connection;

    public static void initialize() throws Exception {
        module = opensilex.getModuleByClass(NoSQLModule.class);
        module.setup();
    }

    
    
      
    @AfterClass
    public static void destroy() throws Exception {
        module.shutdown(); 
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

    
    
}

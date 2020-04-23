//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.nosql;

import org.junit.AfterClass;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.opensilex.nosql.service.NoSQLConnection;
import org.opensilex.unit.test.AbstractUnitTest;

/**
 *
 * @author vincent
 */
public abstract class NoSQLServiceTest extends AbstractUnitTest {

    protected static NoSQLModule NoSQLModule;

    protected static NoSQLConnection connection;

    public static void initialize() throws Exception {
        NoSQLModule = opensilex.getModuleByClass(NoSQLModule.class);
        NoSQLModule.setOpenSilex(opensilex);
        NoSQLModule.setup();
        NoSQLModule.startup();
    }

    @AfterClass
    public static void destroy() throws Exception {
        NoSQLModule.shutdown();
    }

    @Test
    public void fakeTest() {
        // TODO implement real tests
        assertTrue("Fake test", true);
    }

}

//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.front;

import org.junit.AfterClass;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.unit.test.AbstractUnitTest;

/**
 *
 * @author vincent
 */
public abstract class FrontTest extends AbstractUnitTest {

    public static void initialize(FileStorageService service) throws Exception {
        // TODO implement connecion init
    }

    @AfterClass
    public static void destroy() {
        // Implement connection destruction
    }

    @Test
    public void fakeTest() {
        // TODO implement real tests
        assertTrue("Fake test", true);
    }

}

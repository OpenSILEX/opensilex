//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.rdf4j;

import org.junit.BeforeClass;
import org.opensilex.sparql.SPARQLServiceTest;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.SHACLTest;

/**
 *
 * @author vincent
 */
public class RDF4JSHACLTest extends SHACLTest {

    private static SPARQLService sparql;

    @BeforeClass
    public static void initialize() throws Exception {
        sparql = new RDF4JInMemoryService().provide();
        SHACLTest.initialize(sparql);
    }
}

//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.rdf4j;

import org.junit.BeforeClass;
import org.opensilex.sparql.SPARQLServiceTest;

/**
 *
 * @author vincent
 */
public class RDF4JConnectionTest extends SPARQLServiceTest {

    @BeforeClass
    public static void initialize() throws Exception {
        SPARQLServiceTest.initialize(new RDF4JInMemoryService().provide());
    }
}

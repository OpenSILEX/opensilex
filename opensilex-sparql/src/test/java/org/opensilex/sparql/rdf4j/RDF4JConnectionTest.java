//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.rdf4j;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.opensilex.sparql.SPARQLServiceTest;
import org.opensilex.sparql.model.A;
import org.opensilex.sparql.model.B;
import org.opensilex.sparql.model.C;
import org.opensilex.sparql.model.D;

/**
 *
 * @author vincent
 */
public class RDF4JConnectionTest extends SPARQLServiceTest {

    protected static RDF4JInMemoryServiceFactory factory;

    @BeforeClass
    public static void setupSPARQL() throws Exception {
        factory = new RDF4JInMemoryServiceFactory();
        factory.setOpenSilex(opensilex);
        factory.setup();
        factory.startup();
        factory.getMapperIndex().addClasses(
                A.class,
                B.class,
                C.class,
                D.class
        );
        sparql = factory.provide();
        SPARQLServiceTest.initialize();
    }

    @AfterClass
    public static void cleanSPARQL() throws Exception {
        factory.dispose(sparql);
    }
}

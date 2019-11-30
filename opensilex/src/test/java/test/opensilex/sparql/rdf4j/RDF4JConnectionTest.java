//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package test.opensilex.sparql.rdf4j;

import test.opensilex.sparql.SPARQLServiceTest;
import org.eclipse.rdf4j.repository.*;
import org.eclipse.rdf4j.repository.sail.*;
import org.eclipse.rdf4j.sail.memory.*;
import org.eclipse.rdf4j.sail.shacl.*;
import org.junit.*;
import org.opensilex.sparql.*;
import org.opensilex.sparql.rdf4j.*;

/**
 *
 * @author vincent
 */
public class RDF4JConnectionTest extends SPARQLServiceTest {

    @BeforeClass
    public static void initialize() throws Exception {
        Repository repository = new SailRepository(
                new ShaclSail(
                        new MemoryStore()));
        repository.init();

        SPARQLService localService = new SPARQLService(new RDF4JConnection(repository.getConnection()));

//        RDF4JConfig cfg = new RDF4JConfig() {
//            @Override
//            public String serverURI() {
//                return "http://localhost:8080/rdf4j-server";
//            }
//
//            @Override
//            public String repository() {
//                return "tu";
//            }
//
//        };
//
//        SPARQLService localService = new SPARQLService(new RDF4JConnection(cfg));

        SPARQLServiceTest.initialize(localService);
    }
}

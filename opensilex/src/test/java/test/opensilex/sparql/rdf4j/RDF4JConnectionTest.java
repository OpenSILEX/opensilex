//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package test.opensilex.sparql.rdf4j;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.shacl.ShaclSail;
import org.junit.BeforeClass;
import org.opensilex.sparql.SPARQLService;
import org.opensilex.sparql.rdf4j.RDF4JConnection;
import test.opensilex.sparql.SPARQLServiceTest;

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

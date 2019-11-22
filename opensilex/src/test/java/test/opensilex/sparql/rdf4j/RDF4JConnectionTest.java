//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package test.opensilex.sparql.rdf4j;

import java.util.*;
import org.eclipse.rdf4j.repository.*;
import org.eclipse.rdf4j.repository.sail.*;
import org.eclipse.rdf4j.sail.inferencer.fc.*;
import org.eclipse.rdf4j.sail.memory.*;
import org.junit.*;
import org.opensilex.*;
import org.opensilex.sparql.*;
import org.opensilex.sparql.rdf4j.*;
import test.opensilex.sparql.*;

/**
 *
 * @author vincent
 */
public class RDF4JConnectionTest extends SPARQLServiceTest {

    @BeforeClass
    public static void initialize() throws Exception {
        Repository repository = new SailRepository(
                new SchemaCachingRDFSInferencer(
                        new MemoryStore()));
        repository.init();

        service = new SPARQLService(new RDF4JConnection(repository.getConnection()));

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
//        service = new SPARQLService(new RDF4JConnection(cfg));
//        service.startup();
        OpenSilex.setup(new HashMap<String, String>() {
            {
                put(OpenSilex.PROFILE_ID_ARG_KEY, OpenSilex.TEST_PROFILE_ID);
            }
        });

        SPARQLServiceTest.initialize();
    }
}

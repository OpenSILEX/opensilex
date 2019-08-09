/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test.opensilex.sparql.rdf4j;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.inferencer.fc.SchemaCachingRDFSInferencer;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.BeforeClass;
import org.opensilex.sparql.SPARQLService;
import org.opensilex.sparql.exceptions.SPARQLQueryException;
import org.opensilex.sparql.rdf4j.RDF4JConnection;

import test.opensilex.sparql.SPARQLServiceTest;

/**
 *
 * @author vincent
 */
public class RDF4JConnectionTest extends SPARQLServiceTest {

    @BeforeClass
    public static void initialize() throws SPARQLQueryException {
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
//                return "tu3";
//            }
//
//        };
//        
//        service = new SPARQLService(new RDF4JConnection(cfg));

        SPARQLServiceTest.initialize();
    }
}

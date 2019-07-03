/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test.opensilex.module.core.service.sparql.rdf4j;

import test.opensilex.module.core.service.sparql.SPARQLServiceTest;
import org.eclipse.rdf4j.repository.Repository;
import org.opensilex.module.core.service.sparql.rdf4j.RDF4JConfig;
import org.opensilex.module.core.service.sparql.rdf4j.RDF4JConnection;

/**
 *
 * @author vincent
 */
public class RDF4JConnectionTest extends SPARQLServiceTest<RDF4JConnection> {

    private Repository repository;

    @Override
    protected RDF4JConnection createConnection() {
//        repository = new SailRepository(
//			  new SchemaCachingRDFSInferencer(
//			  new MemoryStore()));
//        repository.init();
//
//        return new RDF4JConnection(repository.getConnection());

        RDF4JConfig cfg = new RDF4JConfig() {
            @Override
            public String serverURI() {
                return "http://localhost:8080/rdf4j-server";
            }

            @Override
            public String repository() {
                return "tu3";
            }

        };
        
        return new RDF4JConnection(cfg);
    }

    @Override
    protected void closeConnection() {
//        repository.shutDown();
    }
}

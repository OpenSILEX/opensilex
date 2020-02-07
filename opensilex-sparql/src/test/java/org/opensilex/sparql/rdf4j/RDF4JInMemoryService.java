/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql.rdf4j;

import java.net.URISyntaxException;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.shacl.ShaclSail;
import org.opensilex.sparql.rdf4j.RDF4JConnection;
import org.opensilex.sparql.service.SPARQLService;

/**
 *
 * @author vmigot
 */
public class RDF4JInMemoryService extends SPARQLService {

    public RDF4JInMemoryService() throws URISyntaxException {
        super(getInMemoryConnection());
    }

    private static ShaclSail shacl;

    public static RDF4JConnection getInMemoryConnection() throws URISyntaxException {
        shacl = new ShaclSail(new MemoryStore());
        Repository repository = new SailRepository(shacl);
        repository.init();

        return new RDF4JConnection(repository.getConnection()) {
            @Override
            public void shutdown() {
                shacl = null;
                super.shutdown();
            }
        };
    }

}

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

/**
 *
 * @author vmigot
 */
public class RDF4JInMemoryService extends RDF4JServiceFactory {

    public RDF4JInMemoryService() throws URISyntaxException {
        super(getInMemoryRepository());
    }

    public static Repository getInMemoryRepository() throws URISyntaxException {
        MemoryStore memoryStore = new MemoryStore();
        ShaclSail shacl = new ShaclSail(memoryStore);
        SailRepository repository = new SailRepository(shacl);
        repository.init();

        return repository;
    }

}

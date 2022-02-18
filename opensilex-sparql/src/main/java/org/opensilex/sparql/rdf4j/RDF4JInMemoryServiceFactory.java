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
import org.opensilex.sparql.SPARQLModule;
import org.opensilex.sparql.service.SPARQLService;

/**
 *
 * @author vmigot
 */
public class RDF4JInMemoryServiceFactory extends RDF4JServiceFactory {

    public RDF4JInMemoryServiceFactory(){
        super(getInMemoryRepository());
    }

    public static Repository getInMemoryRepository() {
        MemoryStore memoryStore = new MemoryStore();
        ShaclSail shacl = new ShaclSail(memoryStore);
        SailRepository repository = new SailRepository(shacl);
        repository.init();

        return repository;
    }

    @Override
    public void startup() throws Exception {
        super.startup();
        SPARQLService sparql = this.provide();
        getOpenSilex().getModuleByClass(SPARQLModule.class).installOntologies(sparql, false);
        this.dispose(sparql);
    }

}

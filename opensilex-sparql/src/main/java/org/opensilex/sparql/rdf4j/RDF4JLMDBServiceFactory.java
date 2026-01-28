/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql.rdf4j;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.lmdb.LmdbStore;
import org.eclipse.rdf4j.sail.shacl.ShaclSail;
import org.opensilex.sparql.SPARQLModule;
import org.opensilex.sparql.service.SPARQLService;

/**
 *
 * @author vmigot
 */
public class RDF4JLMDBServiceFactory extends RDF4JServiceFactory {

    public RDF4JLMDBServiceFactory(){
        super(getLMDBRepository());
    }

    public static Repository getLMDBRepository() {
        //@todo To resoluve the `GroupConcat` errors, maybe change the repo config after updating RDF4J to 5.x.x.
        //      See https://rdf4j.org/documentation/programming/repository/#the-repositorymanager-and-repositoryprovider
        LmdbStore lmdbStore = new LmdbStore();
        ShaclSail shacl = new ShaclSail(lmdbStore);
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

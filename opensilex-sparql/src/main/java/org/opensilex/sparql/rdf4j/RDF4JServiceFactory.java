/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql.rdf4j;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.http.HTTPRepository;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.service.SPARQLServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author vince
 */
public class RDF4JServiceFactory extends SPARQLServiceFactory {
    
    private final static Logger LOGGER = LoggerFactory.getLogger(RDF4JServiceFactory.class);
    
    private final Repository repository;

    public RDF4JServiceFactory(RDF4JConfig config) {
        LOGGER.debug("Build RDF4JServiceFactory from config");
        this.repository = new HTTPRepository(config.serverURI(), config.repository());
        this.repository.init();
    }
    
    public RDF4JServiceFactory(Repository repository) {
        LOGGER.debug("Build RDF4JServiceFactory from repository");
        this.repository = repository;
    }

    @Override
    public SPARQLService provide() {
        try {
            RepositoryConnection connection = repository.getConnection();
            SPARQLService sparql = new SPARQLService(new RDF4JConnection(connection));
            sparql.startup();
            return sparql;
        } catch (Exception ex) {
            LOGGER.error("Error while opening RDF4J service connectioninstance", ex);
            return null;
        }
    }

    @Override
    public void dispose(SPARQLService sparql) {
        try {
            sparql.shutdown();
        } catch (Exception ex) {
            LOGGER.error("Error while closing RDF4J service connectioninstance instance", ex);
        }
        
    }
}

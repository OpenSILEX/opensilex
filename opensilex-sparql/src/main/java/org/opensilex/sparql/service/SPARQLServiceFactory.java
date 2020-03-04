/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql.service;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.hk2.api.Factory;
import org.opensilex.service.ServiceConfigDefault;
import org.opensilex.service.ServiceFactory;
import org.opensilex.sparql.mapping.SPARQLClassObjectMapper;
import org.opensilex.sparql.rdf4j.RDF4JConfig;
import org.opensilex.sparql.rdf4j.RDF4JConnection;
import org.slf4j.LoggerFactory;

/**
 *
 * @author vmigot
 */
public class SPARQLServiceFactory extends ServiceFactory<SPARQLService> implements Factory<SPARQLService> {
    
    private final static org.slf4j.Logger LOGGER = LoggerFactory.getLogger(SPARQLServiceFactory.class);
    
    @Override
    public SPARQLService provide() {
        try {
            return this.getNewInstance();
        } catch (Exception ex) {
            LOGGER.error("Error while creating SPARQL service instance", ex);
            return null;
        }
    }
    
    @Override
    public void dispose(SPARQLService sparql) {
        sparql.closeConnection();
    }
    
}

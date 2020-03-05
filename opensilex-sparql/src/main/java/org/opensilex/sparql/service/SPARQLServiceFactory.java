/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql.service;

import javax.ws.rs.ext.Provider;
import org.opensilex.service.ServiceConfigDefault;
import org.opensilex.service.ServiceFactory;
import org.opensilex.sparql.rdf4j.RDF4JConfig;
import org.opensilex.sparql.rdf4j.RDF4JServiceFactory;

/**
 *
 * @author vince
 */
@ServiceConfigDefault(
        implementation = RDF4JServiceFactory.class,
        configClass = RDF4JConfig.class,
        configID = "rdf4j"
)
@Provider
public abstract class SPARQLServiceFactory extends ServiceFactory<SPARQLService> {

    public Class<SPARQLService> getServiceClass() {
        return SPARQLService.class;   
    }
    
}

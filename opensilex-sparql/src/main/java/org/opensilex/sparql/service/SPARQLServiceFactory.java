/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql.service;

import java.net.URI;
import javax.ws.rs.ext.Provider;
import org.opensilex.OpenSilex;
import org.opensilex.module.ModuleNotFoundException;
import org.opensilex.service.ServiceDefaultDefinition;
import org.opensilex.service.ServiceFactory;
import org.opensilex.sparql.SPARQLModule;
import org.opensilex.sparql.mapping.SPARQLClassObjectMapperIndex;
import org.opensilex.sparql.rdf4j.RDF4JServiceFactory;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author vince
 */
@ServiceDefaultDefinition(
        //        implementation = RDF4JInMemoryServiceFactory.class
        implementation = RDF4JServiceFactory.class
)
@Provider
public abstract class SPARQLServiceFactory extends ServiceFactory<SPARQLService> {

    private final static Logger LOGGER = LoggerFactory.getLogger(SPARQLServiceFactory.class);

    public Class<SPARQLService> getServiceClass() {
        return SPARQLService.class;
    }

    public abstract void createRepository() throws Exception;

    public abstract void deleteRepository() throws Exception;

    public String getDefaultLanguage() {
        if (getOpenSilex() == null) {
            return OpenSilex.DEFAULT_LANGUAGE;
        }
        return getOpenSilex().getDefaultLanguage();
    }

    public URI getBaseURI() {
        if (getOpenSilex() == null) {
            return SPARQLModule.getDefaultPlatformURI();
        }
        try {
            return getOpenSilex().getModuleByClass(SPARQLModule.class).getPlatformURI();
        } catch (ModuleNotFoundException ex) {
            LOGGER.error("Error while retriving platform URI from configuration (using default)", ex);
            return SPARQLModule.getDefaultPlatformURI();
        }
    }

    protected SPARQLClassObjectMapperIndex mapperIndex;

    public SPARQLClassObjectMapperIndex getMapperIndex() throws Exception {
        if (mapperIndex == null) {
            Reflections reflections;
            if (getOpenSilex() == null) {
                reflections = OpenSilex.createReflections();
            } else {
                reflections = getOpenSilex().getReflections();
            }
            mapperIndex = new SPARQLClassObjectMapperIndex(getBaseURI(), reflections);
        }
        return mapperIndex;
    }
}

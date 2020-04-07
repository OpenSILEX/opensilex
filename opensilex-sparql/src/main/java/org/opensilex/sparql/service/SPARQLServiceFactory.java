/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql.service;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import javax.ws.rs.ext.Provider;
import org.opensilex.OpenSilex;
import org.opensilex.OpenSilexModuleNotFoundException;
import org.opensilex.service.ServiceDefaultDefinition;
import org.opensilex.service.ServiceFactory;
import org.opensilex.sparql.SPARQLModule;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.mapping.SPARQLClassObjectMapperIndex;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.rdf4j.RDF4JServiceFactory;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ConfigurationBuilder;
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
        } catch (OpenSilexModuleNotFoundException ex) {
            LOGGER.error("Error while retriving platform URI from configuration (using default)", ex);
            return SPARQLModule.getDefaultPlatformURI();
        }
    }

    protected static SPARQLClassObjectMapperIndex mapperIndex;

    public void resetMapperIndex() throws Exception {
        mapperIndex = null;
    }

    public SPARQLClassObjectMapperIndex getMapperIndex() throws Exception {

        if (mapperIndex == null) {
            URI baseURI = getBaseURI();
            Reflections reflections;
            LOGGER.debug("Build SPARQL models for base URI: " + baseURI.toString());
            if (getOpenSilex() == null) {
                reflections = new Reflections(ConfigurationBuilder.build("")
                        .addClassLoader(Thread.currentThread().getContextClassLoader())
                        .setScanners(new TypeAnnotationsScanner(), new SubTypesScanner(), new MethodAnnotationsScanner())
                        .setExpandSuperTypes(false));;
            } else {
                reflections = getOpenSilex().getReflections();
            }
            Set<Class<? extends SPARQLResourceModel>> initClasses = new HashSet<>();
            reflections.getTypesAnnotatedWith(SPARQLResource.class).forEach(c -> {
                LOGGER.debug("Register model class to build: " + c.getCanonicalName());
                initClasses.add((Class<? extends SPARQLResourceModel>) c);
            });
            mapperIndex = new SPARQLClassObjectMapperIndex(getBaseURI(), initClasses);
        }
        return mapperIndex;
    }
}

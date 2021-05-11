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
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.Resource;
import org.opensilex.OpenSilex;
import org.opensilex.service.ServiceConfig;
import org.opensilex.service.ServiceDefaultDefinition;
import org.opensilex.service.ServiceFactory;
import org.opensilex.sparql.SPARQLConfig;
import org.opensilex.sparql.SPARQLModule;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.extensions.OntologyFileDefinition;
import org.opensilex.sparql.extensions.SPARQLExtension;
import org.opensilex.sparql.mapping.SPARQLClassObjectMapper;
import org.opensilex.sparql.mapping.SPARQLClassObjectMapperIndex;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.rdf4j.RDF4JServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author vince
 */
@Provider
@ServiceDefaultDefinition(implementation = RDF4JServiceFactory.class)
public abstract class SPARQLServiceFactory extends ServiceFactory<SPARQLService> {

    private final static Logger LOGGER = LoggerFactory.getLogger(SPARQLServiceFactory.class);

    public SPARQLServiceFactory(ServiceConfig config) {
        super(config);
    }

    @Override
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

    private URI baseURI;

    private URI generationPrefixURI;

    protected SPARQLClassObjectMapperIndex mapperIndex;

    private SPARQLModule sparqlModule;

    @Override
    public void setup() throws Exception {
        sparqlModule = getOpenSilex().getModuleByClass(SPARQLModule.class);
        baseURI = sparqlModule.getBaseURI();
        generationPrefixURI = sparqlModule.getGenerationPrefixURI();
    }

    @Override
    public void startup() throws Exception {
        LOGGER.debug("Build SPARQL models for base URI: " + baseURI.toString());

        Set<Class<? extends SPARQLResourceModel>> initClasses = new HashSet<>();

        getOpenSilex().getAnnotatedClasses(SPARQLResource.class).forEach(c -> {
            LOGGER.debug("Register model class to build: " + c.getCanonicalName());
            initClasses.add((Class<? extends SPARQLResourceModel>) c);
        });
        mapperIndex = new SPARQLClassObjectMapperIndex(baseURI, generationPrefixURI, initClasses);

        SPARQLConfig sparqlConfig = sparqlModule.getConfig(SPARQLConfig.class);
        if (sparqlConfig.usePrefixes()) {
            mapperIndex.forEach((Resource resource, SPARQLClassObjectMapper<?> mapper) -> {
                String resourceNamespace = mapper.getResourceGraphNamespace();
                String resourcePrefix = mapper.getResourceGraphPrefix();
                if (resourceNamespace != null && resourcePrefix != null && !resourcePrefix.isEmpty()) {
                    SPARQLService.addPrefix(sparqlModule.getBaseURIAlias().toString() + mapper.getResourceGraphPrefix(), resourceNamespace + "#");
                }
            });

            SPARQLService.addPrefix(sparqlConfig.baseURIAlias(), sparqlConfig.baseURI());

            if (!StringUtils.isBlank(sparqlConfig.generationBaseURI())
                    && !sparqlModule.getGenerationPrefixURI().equals(sparqlModule.getBaseURI().toString())
                    && !sparqlConfig.baseURIAlias().equals(sparqlConfig.generationBaseURIAlias())) {
                SPARQLService.addPrefix(sparqlConfig.generationBaseURIAlias(), sparqlConfig.generationBaseURI());
            }

            for (SPARQLExtension module : getOpenSilex().getModulesImplementingInterface(SPARQLExtension.class)) {
                for (OntologyFileDefinition ontologyDef : module.getOntologiesFiles()) {
                    SPARQLService.addPrefix(ontologyDef.getPrefix(), ontologyDef.getPrefixUri().toString());
                }
            }

        } else {
            SPARQLService.clearPrefixes();
        }

        URIDeserializer.setPrefixes(SPARQLService.getPrefixMapping(), sparqlConfig.usePrefixes());
    }

    public void shutdown() {
        SPARQLService.clearPrefixes();
        URIDeserializer.clearPrefixes();
    }

    public SPARQLClassObjectMapperIndex getMapperIndex() throws Exception {
        return mapperIndex;
    }

}

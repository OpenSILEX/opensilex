/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql;

import org.opensilex.sparql.service.SPARQLService;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import org.opensilex.OpenSilexModule;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.exceptions.SPARQLValidationException;
import org.opensilex.sparql.extensions.SPARQLExtension;
import org.opensilex.sparql.rdf4j.RDF4JInMemoryServiceFactory;
import org.opensilex.sparql.service.SPARQLServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author vince
 */
public class SPARQLModule extends OpenSilexModule {

    private final static String DEFAULT_BASE_URI = "http://default.opensilex.org/";

    private final static Logger LOGGER = LoggerFactory.getLogger(SPARQLModule.class);

    public final static String ONTOLOGY_BASE_DOMAIN = "http://www.opensilex.org/";

    @Override
    public Class<?> getConfigClass() {
        return SPARQLConfig.class;
    }

    @Override
    public String getConfigId() {
        return "ontologies";
    }

    private URI baseURI;

    private String baseURIAlias;

    private URI generationPrefixURI;
    
    private SPARQLConfig sparqlConfig;

    private Map<String, URI> customPrefixes = new HashMap<>();

    @Override
    public void setup() throws Exception {
        sparqlConfig = this.getConfig(SPARQLConfig.class);
        if (sparqlConfig != null) {
            baseURIAlias = sparqlConfig.baseURIAlias() + "-";
            baseURI = new URI(sparqlConfig.baseURI());
            sparqlConfig.customPrefixes().forEach((prefix, uri) -> {
                try {
                    if (URIDeserializer.validateURI(uri)) {
                        customPrefixes.put(prefix, new URI(uri));
                    } else {
                        throw new RuntimeException("Invalid custom uri prefix: " + prefix + " - " + uri);
                    }
                } catch (URISyntaxException ex) {
                    throw new RuntimeException("Invalid custom uri prefix: " + prefix + " - " + uri, ex);
                }
            });
        } else {
            baseURIAlias = "";
            baseURI = new URI(DEFAULT_BASE_URI);
        }

        LOGGER.debug("Set platform URI: " + baseURI.toString());
        LOGGER.debug("Set platform URI alias: " + baseURIAlias);
        if (sparqlConfig != null && !sparqlConfig.generationBaseURI().isBlank()) {
            generationPrefixURI = new URI(sparqlConfig.generationBaseURI());
            LOGGER.debug("Set platform base URI for auto-generated URI: " + generationPrefixURI.toString());
        } else {
            generationPrefixURI = baseURI;
        }

    }

    public String getBaseURIAlias() {
        return baseURIAlias;
    }

    public URI getBaseURI() {
        return baseURI;
    }

    public URI getGenerationPrefixURI() {
        return generationPrefixURI;
    }

    public URI getSuffixedURI(String suffix) {
        return baseURI.resolve(suffix);
    }

    public Map<String, URI> getCustomPrefixes() {
        return customPrefixes;
    }

    public void installOntologies(SPARQLService sparql, boolean reset) throws Exception {
        try {
            sparql.disableSHACL();
            // Allow any module implementing SPARQLExtension to add custom ontologies
            for (SPARQLExtension module : getOpenSilex().getModulesImplementingInterface(SPARQLExtension.class)) {
                module.installOntologies(sparql, reset);
            }
        } catch (Exception ex) {
            throw ex;
        }
    }

    @Override
    public void install(boolean reset) throws Exception {
        SPARQLConfig cfg = this.getConfig(SPARQLConfig.class);
        SPARQLServiceFactory sparql = cfg.sparql();
        if (reset) {
            sparql.deleteRepository();
        }
        sparql.createRepository();
        sparql = cfg.sparql();

        SPARQLService sparqlService = sparql.provide();

        try {
            installOntologies(sparqlService, reset);

            if (cfg.enableSHACL()) {
                try {
                    sparqlService.enableSHACL();
                } catch (SPARQLValidationException ex) {
                    LOGGER.warn("Error while enable SHACL validation:");
                    LOGGER.warn(ex.getMessage());
                }
            } else {
                sparqlService.disableSHACL();
            }
        } catch (Exception ex) {

            LOGGER.error("Error while initializing SHACL", ex);
        } finally {
            sparql.dispose(sparqlService);
        }

    }

    @Override
    public void check() throws Exception {
        LOGGER.info("Check SPARQL required ontologies");
        SPARQLServiceFactory factory = getOpenSilex().getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class);
        SPARQLService sparql = factory.provide();
        for (SPARQLExtension module : getOpenSilex().getModulesImplementingInterface(SPARQLExtension.class)) {
            module.checkOntologies(sparql);
        }
        factory.dispose(sparql);
    }

    @Override
    public void startup() throws Exception {
        SPARQLServiceFactory factory = getOpenSilex().getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class);
        if (factory instanceof RDF4JInMemoryServiceFactory) {
            for (SPARQLExtension module : getOpenSilex().getModulesImplementingInterface(SPARQLExtension.class)) {
                module.inMemoryInitialization();
            }
        }
    }

}

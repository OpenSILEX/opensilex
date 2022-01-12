/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql;

import org.opensilex.OpenSilex;
import org.opensilex.OpenSilexModuleNotFoundException;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.ontology.dal.OntologyDAO;
import org.opensilex.sparql.ontology.store.DefaultOntologyStore;
import org.opensilex.sparql.ontology.store.NoOntologyStore;
import org.opensilex.sparql.ontology.store.OntologyStore;
import org.opensilex.sparql.service.SPARQLService;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.opensilex.OpenSilexModule;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.exceptions.SPARQLValidationException;
import org.opensilex.sparql.extensions.SPARQLExtension;
import org.opensilex.sparql.rdf4j.RDF4JInMemoryServiceFactory;
import org.opensilex.sparql.service.SPARQLServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.UriBuilder;

/**
 * @author vince
 */
public class SPARQLModule extends OpenSilexModule {

    private static final Logger LOGGER = LoggerFactory.getLogger(SPARQLModule.class);

    private static final String DEFAULT_BASE_URI = "http://default.opensilex.org/";
    public static final String ONTOLOGY_BASE_DOMAIN = "http://www.opensilex.org/";

    private URI baseURI;
    private String baseURIAlias;
    private URI generationPrefixURI;

    private final Map<String, URI> customPrefixes = new HashMap<>();

    private static OntologyStore ontologyStore;

    @Override
    public Class<?> getConfigClass() {
        return SPARQLConfig.class;
    }

    @Override
    public String getConfigId() {
        return "ontologies";
    }


    @Override
    public void setup() throws Exception {
        SPARQLConfig sparqlConfig = this.getConfig(SPARQLConfig.class);
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

        generationPrefixURI = baseURI;

        if (sparqlConfig != null) {
            // if some generation URI is provided then use it
            if (!StringUtils.isEmpty(sparqlConfig.generationBaseURI())) {
                generationPrefixURI = new URI(sparqlConfig.generationBaseURI());
            }
            // else generate URI with the provided uri generation alias
            else if (!StringUtils.isEmpty(sparqlConfig.generationBaseURIAlias())) {
                // use UriBuilder in order to properly create URI
                generationPrefixURI = UriBuilder.fromUri(baseURI).path(sparqlConfig.generationBaseURIAlias()).build();
            }
        }

        LOGGER.debug("Set platform base URI for auto-generated URI: " + generationPrefixURI);
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
        // #TODO clean cache
        sparql.disableSHACL();
        // Allow any module implementing SPARQLExtension to add custom ontologies
        for (SPARQLExtension module : getOpenSilex().getModulesImplementingInterface(SPARQLExtension.class)) {
            module.installOntologies(sparql, reset);
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
                sparqlService.enableSHACL();
            } else {
                sparqlService.disableSHACL();
            }
        } catch (SPARQLValidationException ex) {
            LOGGER.warn("Error while enable SHACL validation:");
            LOGGER.warn(ex.getMessage());
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

    private static final String ONTOLOGY_STORE_LOAD_SUCCESS_MSG = "Ontology store loaded with success. Duration: {} ms";


    private static void initOntologyStore(OpenSilex openSilex, SPARQLService sparql) throws SPARQLException, OpenSilexModuleNotFoundException {

        if (sparql == null) {
            return;
        }

        SPARQLConfig sparqlConfig = openSilex.getModuleConfig(SPARQLModule.class, SPARQLConfig.class);

        if (sparqlConfig.enableOntologyStore()) {
            Instant begin = Instant.now();

            ontologyStore = new DefaultOntologyStore(sparql, openSilex);
            ontologyStore.load();

            long durationMs = Duration.between(begin, Instant.now()).toMillis();
            LOGGER.info(ONTOLOGY_STORE_LOAD_SUCCESS_MSG, durationMs);
        } else {
            ontologyStore = new NoOntologyStore(new OntologyDAO(sparql));
        }

    }

    public static OntologyStore getOntologyStoreInstance() {
        return ontologyStore;
    }

    @Override
    public void startup() throws Exception {
        SPARQLServiceFactory factory = getOpenSilex().getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class);
        if (factory instanceof RDF4JInMemoryServiceFactory) {
            for (SPARQLExtension module : getOpenSilex().getModulesImplementingInterface(SPARQLExtension.class)) {
                module.inMemoryInitialization();
            }
        }

        SPARQLService sparql = factory.provide();
        SPARQLModule.initOntologyStore(getOpenSilex(), sparql);
    }

}

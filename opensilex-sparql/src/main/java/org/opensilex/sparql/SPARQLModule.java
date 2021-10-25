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
import org.apache.commons.lang3.StringUtils;
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

    private static final String DEFAULT_BASE_URI = "http://default.opensilex.org/";

    private static final Logger LOGGER = LoggerFactory.getLogger(SPARQLModule.class);

    public static final String ONTOLOGY_BASE_DOMAIN = "http://www.opensilex.org/";

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

    private URI generationUri;
    
    private final Map<String, URI> customPrefixes = new HashMap<>();

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

        if(sparqlConfig != null){
            // if some generation URI is provided then use it
            if(! StringUtils.isEmpty(sparqlConfig.generationBaseURI())){
                generationUri = new URI(sparqlConfig.generationBaseURI());
            }
            // else generate URI with the provided uri generation alias
            else  if(!StringUtils.isEmpty(sparqlConfig.generationBaseURIAlias())){
                generationUri = generationUri.resolve(sparqlConfig.generationBaseURIAlias()).resolve("/");
            }else {
                generationUri = baseURI;
            }
        }
        else {
            generationUri = baseURI;
        }
        LOGGER.debug("Set platform base URI for auto-generated URI: " + generationUri);
    }

    public String getBaseURIAlias() {
        return baseURIAlias;
    }

    public URI getBaseURI() {
        return baseURI;
    }

    public URI getGenerationUri() {
        return generationUri;
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
        }
        catch (SPARQLValidationException ex){
            LOGGER.warn("Error while enable SHACL validation:");
            LOGGER.warn(ex.getMessage());
        }
        catch (Exception ex) {
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

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql;

import org.apache.jena.riot.Lang;
import org.apache.jena.vocabulary.DCTerms;
import org.eclipse.rdf4j.model.vocabulary.DC;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.opensilex.sparql.extensions.OntologyFileDefinition;
import org.opensilex.sparql.model.time.Time;
import org.opensilex.sparql.service.SPARQLService;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

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
 *
 * @author vince
 */
public class SPARQLModule extends OpenSilexModule implements SPARQLExtension{

    private static final String DEFAULT_BASE_URI = "http://default.opensilex.org/";

    private static final Logger LOGGER = LoggerFactory.getLogger(SPARQLModule.class);

    public static final String ONTOLOGY_BASE_DOMAIN = "http://www.opensilex.org/";
    public static final String ONTOLOGIES_DIRECTORY = "ontologies";

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

        generationPrefixURI = baseURI;

        if(sparqlConfig != null){
            // if some generation URI is provided then use it
            if(! StringUtils.isEmpty(sparqlConfig.generationBaseURI())){
                generationPrefixURI = new URI(sparqlConfig.generationBaseURI());
            }
            // else generate URI with the provided uri generation alias
            else  if(!StringUtils.isEmpty(sparqlConfig.generationBaseURIAlias())){
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

    @Override
    public List<OntologyFileDefinition> getOntologiesFiles() throws Exception {
        List<OntologyFileDefinition> list = new LinkedList<>();

        list.add(new OntologyFileDefinition(
                RDFS.NAMESPACE,
                ONTOLOGIES_DIRECTORY+"/rdfs.ttl",
                Lang.TURTLE,
                RDFS.PREFIX
        ));
        list.add(new OntologyFileDefinition(
                OWL.NAMESPACE,
                ONTOLOGIES_DIRECTORY+"/owl2.ttl",
                Lang.TURTLE,
                OWL.PREFIX
        ));
        list.add(new OntologyFileDefinition(
                Time.NS,
                ONTOLOGIES_DIRECTORY+"/time.ttl",
                Lang.TURTLE,
                Time.PREFIX
        ));

        // https://www.dublincore.org/schemas/rdfs/ -> https://www.dublincore.org/specifications/dublin-core/dcmi-terms/dublin_core_terms.ttl
        list.add(new OntologyFileDefinition(
                DCTerms.NS,
                ONTOLOGIES_DIRECTORY+"/dc.ttl",
                Lang.TURTLE,
                DC.PREFIX
        ));
        return list;
    }

    @Override
    public void installOntologies(SPARQLService sparql, boolean reset) throws Exception {
        // #TODO clean cache
        sparql.disableSHACL();
        // Allow any module implementing SPARQLExtension to add custom ontologies
        for (SPARQLExtension module : getOpenSilex().getModulesImplementingInterface(SPARQLExtension.class)) {

            // don't call method on SPARQLModule in order to avoid infinite recursion
            if(! module.getClass().equals(SPARQLModule.class)){
                module.installOntologies(sparql, reset);
            }
        }

        // use SPARQLExtension default behavior
        ((SPARQLExtension) this).installOntologies(sparql,reset);

        SPARQLService.addPrefix(XSD.PREFIX, XSD.NAMESPACE,this);
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

            // don't call method on SPARQLModule in order to avoid infinite recursion
            if(! module.getClass().equals(SPARQLModule.class)){
                module.checkOntologies(sparql);
            }
        }

        // use SPARQLExtension default behavior
        checkOntologies(sparql);

        factory.dispose(sparql);
    }

    @Override
    public void startup() throws Exception {
        SPARQLServiceFactory factory = getOpenSilex().getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class);
        if (factory instanceof RDF4JInMemoryServiceFactory) {
            for (SPARQLExtension module : getOpenSilex().getModulesImplementingInterface(SPARQLExtension.class)) {

                // don't call method on SPARQLModule in order to avoid infinite recursion
                if(! module.getClass().equals(SPARQLModule.class)){
                    module.inMemoryInitialization();
                }
            }
        }
    }

}

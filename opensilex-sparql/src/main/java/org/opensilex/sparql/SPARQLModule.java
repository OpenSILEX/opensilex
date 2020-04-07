/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql;

import java.io.FileInputStream;
import java.io.InputStream;
import org.opensilex.sparql.service.SPARQLService;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import org.apache.jena.rdf.model.Resource;
import org.opensilex.OpenSilexModule;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.exceptions.SPARQLValidationException;
import org.opensilex.sparql.extensions.OntologyFileDefinition;
import org.opensilex.sparql.extensions.SPARQLExtension;
import org.opensilex.sparql.mapping.SPARQLClassObjectMapper;
import org.opensilex.sparql.service.SPARQLServiceFactory;
import org.opensilex.sparql.service.SPARQLStatement;
import org.opensilex.utils.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author vince
 */
public class SPARQLModule extends OpenSilexModule {

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

    @Override
    public void startup() throws Exception {
        final String basePrefix;
        SPARQLConfig sparqlConfig = this.getConfig(SPARQLConfig.class);
        if (sparqlConfig != null) {
            basePrefix = sparqlConfig.baseURIAlias() + "-";
        } else {
            basePrefix = "";
        }

        platformURI = new URI(sparqlConfig.baseURI());

        if (sparqlConfig.usePrefixes()) {
            SPARQLServiceFactory sparqlFactory = getOpenSilex().getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class);
            sparqlFactory.getMapperIndex().forEach((Resource resource, SPARQLClassObjectMapper<?> mapper) -> {
                String resourceNamespace = mapper.getResourceGraphNamespace();
                String resourcePrefix = mapper.getResourceGraphPrefix();
                if (resourceNamespace != null && resourcePrefix != null && !resourcePrefix.isEmpty()) {
                    SPARQLService.addPrefix(basePrefix + mapper.getResourceGraphPrefix(), resourceNamespace + "#");
                }
            });

            SPARQLConfig cfg = this.getConfig(SPARQLConfig.class);
            SPARQLService.addPrefix(cfg.baseURIAlias(), cfg.baseURI());

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

    public void installOntologies(SPARQLService sparql, boolean reset) throws Exception {
        try {
            sparql.startTransaction();
            // Allow any module implementing SPARQLExtension to add custom ontologies
            for (SPARQLExtension module : getOpenSilex().getModulesImplementingInterface(SPARQLExtension.class)) {
                for (OntologyFileDefinition ontologyDef : module.getOntologiesFiles()) {
                    if (reset) {
                        sparql.clearGraph(ontologyDef.getUri());
                    }
                    InputStream ontologyStream = new FileInputStream(ClassUtils.getFileFromClassArtifact(module.getClass(), ontologyDef.getFilePath()));
                    sparql.loadOntology(ontologyDef.getUri(), ontologyStream, ontologyDef.getFileType());
                    ontologyStream.close();
                }
            }
            sparql.commitTransaction();
        } catch (Exception ex) {
            sparql.rollbackTransaction();
            throw ex;
        }
    }

    @Override
    public void shutdown() {
        SPARQLService.clearPrefixes();
    }

    private URI platformURI;

    /**
     * Return configured platform base URI
     *
     * @return plateform URI
     */
    public URI getPlatformURI() {
        if (platformURI == null) {
            platformURI = getDefaultPlatformURI();
        }

        return platformURI;
    }

    public static URI getDefaultPlatformURI() {
        try {
            return new URI("http://test.opensilex.org/");
        } catch (URISyntaxException ex) {
            // Silently ignore never happending error
        }
        return null;
    }

    public URI getPlatformDomainGraphURI(String graphSuffix) {
        return getPlatformURI().resolve(graphSuffix);
    }

    public void clearPlatformGraphs(SPARQLService sparql, List<String> graphsSuffixToClear) throws SPARQLException {
        for (String graphName : graphsSuffixToClear) {
            sparql.clearGraph(getPlatformDomainGraphURI(graphName));
        }
        sparql.clearGraph(getPlatformURI());
    }

    @Override
    public void install(boolean reset) throws Exception {
        SPARQLConfig cfg = this.getConfig(SPARQLConfig.class);
        SPARQLServiceFactory sparql = cfg.sparql();
        if (reset) {
            sparql.deleteRepository();
        }
        sparql.createRepository();
        getOpenSilex().shutdown();
        getOpenSilex().startupIfNeed();

        SPARQLService sparqlService = sparql.provide();

        try {
            installOntologies(sparqlService, reset);

            if (cfg.enableSHACL()) {
                try {
                    sparqlService.enableSHACL();
                } catch (SPARQLValidationException ex) {
                    LOGGER.warn("Error while enable SHACL validation:");
                    Map<URI, Map<URI, List<URI>>> errors = ex.getValidationErrors();
                    errors.forEach((URI uri, Map<URI, List<URI>> error) -> {
                        LOGGER.warn("--> " + uri + ":");
                        error.forEach((URI protpertyUri, List<URI> brokenConstraints) -> {
                            LOGGER.warn("    " + protpertyUri + ":");
                            brokenConstraints.forEach(constraintURI -> {
                                LOGGER.warn("      " + constraintURI);
                            });
                        });
                    });
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
            for (OntologyFileDefinition ontologyDef : module.getOntologiesFiles()) {
                List<SPARQLStatement> results = sparql.getGraphStatement(ontologyDef.getUri());

                if (results.size() == 0) {
                    String errorMsg = ontologyDef.getUri().toString() + " is missing data into your triple store, did you execute `opensilex system setup` command ?";
                    LOGGER.warn("/!\\ " + errorMsg);
                    throw new Exception(errorMsg);
                }
            }
        }
        factory.dispose(sparql);
    }
}

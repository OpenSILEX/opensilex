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
import org.opensilex.OpenSilex;
import org.opensilex.module.ModuleNotFoundException;
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

        if (sparqlConfig.usePrefixes()) {
            SPARQLClassObjectMapper.forEach((Resource resource, SPARQLClassObjectMapper<?> mapper) -> {
                String resourceNamespace = mapper.getResourceGraphNamespace();
                String resourcePrefix = mapper.getResourceGraphPrefix();
                if (resourceNamespace != null && resourcePrefix != null && !resourcePrefix.isEmpty()) {
                    SPARQLService.addPrefix(basePrefix + mapper.getResourceGraphPrefix(), resourceNamespace + "#");
                }
            });

            SPARQLConfig cfg = this.getConfig(SPARQLConfig.class);
            SPARQLService.addPrefix(cfg.baseURIAlias(), cfg.baseURI());

            for (SPARQLExtension module : OpenSilex.getInstance().getModulesImplementingInterface(SPARQLExtension.class)) {
                for (OntologyFileDefinition ontologyDef : module.getOntologiesFiles()) {
                    SPARQLService.addPrefix(ontologyDef.getPrefix(), ontologyDef.getPrefixUri().toString());
                }
            }

        } else {
            SPARQLService.clearPrefixes();
        }

        URIDeserializer.setPrefixes(SPARQLService.getPrefixMapping(), sparqlConfig.usePrefixes());
    }

    public static void installOntologies(SPARQLService sparql, boolean reset) throws Exception {
        try {
            sparql.startTransaction();
            // Allow any module implementing SPARQLExtension to add custom ontologies
            for (SPARQLExtension module : OpenSilex.getInstance().getModulesImplementingInterface(SPARQLExtension.class)) {
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
        SPARQLClassObjectMapper.reset();
    }

    /**
     * Return configured platform base URI
     *
     * @return plateform URI
     */
    public static URI getPlatformURI() {
        try {
            if (OpenSilex.getInstance() == null) {
                return new URI("http://test.opensilex.org/");
            } else {
                return new URI(OpenSilex.getInstance().getModuleByClass(SPARQLModule.class).getConfig(SPARQLConfig.class).baseURI());
            }
        } catch (ModuleNotFoundException ex) {
            LOGGER.error("Base module not found, can't really happend because it's in the same package", ex);
            return null;
        } catch (URISyntaxException ex) {
            LOGGER.error("Invalid Base URI", ex);
            return null;
        }
    }

    public static URI getPlatformDomainGraphURI(String graphSuffix) {
        return getPlatformURI().resolve(graphSuffix);
    }

    public static void clearPlatformGraphs(SPARQLService sparql, List<String> graphsSuffixToClear) throws SPARQLException {
        for (String graphName : graphsSuffixToClear) {
            sparql.clearGraph(SPARQLModule.getPlatformDomainGraphURI(graphName));
        }
        sparql.clearGraph(SPARQLModule.getPlatformURI());
    }

    @Override
    public void install(boolean reset) throws Exception {
        SPARQLConfig cfg = this.getConfig(SPARQLConfig.class);
        SPARQLServiceFactory sparql = cfg.sparql();
        if (reset) {
            sparql.deleteRepository();
        }
        sparql.createRepository();
        OpenSilex.getInstance().restart();

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
        SPARQLServiceFactory factory = OpenSilex.getInstance().getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class);
        SPARQLService sparql = factory.provide();
        for (SPARQLExtension module : OpenSilex.getInstance().getModulesImplementingInterface(SPARQLExtension.class)) {
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

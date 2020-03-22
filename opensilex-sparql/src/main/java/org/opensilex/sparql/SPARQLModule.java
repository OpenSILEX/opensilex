/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import org.opensilex.sparql.service.SPARQLService;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.eclipse.rdf4j.common.io.IOUtil;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.config.ConfigTemplate;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.repository.config.RepositoryConfigSchema;
import org.eclipse.rdf4j.repository.config.RepositoryFactory;
import org.eclipse.rdf4j.repository.config.RepositoryRegistry;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;
import org.eclipse.rdf4j.repository.manager.RepositoryProvider;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.opensilex.OpenSilex;
import org.opensilex.module.ModuleConfig;
import org.opensilex.module.ModuleNotFoundException;
import org.opensilex.OpenSilexModule;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.mapping.SPARQLClassObjectMapper;
import org.opensilex.sparql.rdf4j.RDF4JConfig;
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
    public Class<? extends ModuleConfig> getConfigClass() {
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
        } else {
            SPARQLService.clearPrefixes();
        }
        URIDeserializer.setPrefixes(SPARQLService.getPrefixMapping());

        SPARQLConfig cfg = OpenSilex.getModuleConfig(SPARQLModule.class, SPARQLConfig.class);
        SPARQLService.addPrefix(cfg.baseURIAlias(), cfg.baseURI());
        
        SPARQLServiceFactory factory = sparqlConfig.sparql();
        SPARQLService sparql = factory.provide();
        sparql.enableSHACL();
        factory.dispose(sparql);
    }

    @Override
    public void shutdown() {
        SPARQLService.clearPrefixes();
        URIDeserializer.setPrefixes(SPARQLService.getPrefixMapping());
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
    }

    @Override
    public void check() throws Exception {
        LOGGER.info("Check RDF4J connnection");
        SPARQLServiceFactory factory = OpenSilex.getInstance().getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class);
        SPARQLService sparql = factory.provide();
        sparql.getGraphStatement(new URI("http://www.opensilex.org/vocabulary/oeso"));
        factory.dispose(sparql);
    }
}

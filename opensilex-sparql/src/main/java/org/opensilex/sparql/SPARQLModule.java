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
import org.opensilex.sparql.exceptions.SPARQLQueryException;
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

    public static void clearPlatformGraphs(SPARQLService sparql, List<String> graphsSuffixToClear) throws SPARQLQueryException {
        for (String graphName : graphsSuffixToClear) {
            sparql.clearGraph(SPARQLModule.getPlatformDomainGraphURI(graphName));
        }
        sparql.clearGraph(SPARQLModule.getPlatformURI());
    }

    @Override
    public void install(boolean reset) throws Exception {
        LOGGER.info("Initialize RDF4J");
        initRDF4J(reset);
    }
    
    @Override
    public void check() throws Exception {
        LOGGER.info("Check RDF4J connnection & ontologies initialization");
        SPARQLServiceFactory factory = OpenSilex.getInstance().getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class);
        SPARQLService sparql = factory.provide();
        List<SPARQLStatement> results = sparql.describe(new URI("http://www.opensilex.org/vocabulary/oeso"));
        factory.dispose(sparql);
        
        if (results.size() == 0) {
            LOGGER.error("There is missing data into your triple store, did you execute 'opensilex system setup' command ?");
            throw new Exception("There is missing data into your triple store, did you execute 'opensilex system setup' command ?");
        }
    }

    public static void initRDF4J(boolean reset) throws Exception {
        OpenSilex opensilex = OpenSilex.getInstance();
        
        // initialise repository manager
        RDF4JConfig config = opensilex.loadConfigPath("ontologies.sparql.rdf4j", RDF4JConfig.class);

        // Create repository
        createRDF4JRepository(config, reset);

        // Restart repository to reload sparql service
        opensilex.restart();
        SPARQLServiceFactory factory = OpenSilex.getInstance().getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class);
        SPARQLService sparql = factory.provide();

        // Import default ontologies
        LOGGER.info("Install oa ontology: http://www.w3.org/ns/oa");
        InputStream ontologyStream = new FileInputStream(ClassUtils.getFileFromClassArtifact(SPARQLModule.class, "install/oa.rdf"));
        sparql.loadOntologyStream(new URI("http://www.w3.org/ns/oa"), ontologyStream, Lang.RDFXML);
        ontologyStream.close();

        LOGGER.info("Install oeso ontology: http://www.opensilex.org/vocabulary/oeso");
        ontologyStream = new FileInputStream(ClassUtils.getFileFromClassArtifact(SPARQLModule.class, "install/oeso.owl"));
        sparql.loadOntologyStream(new URI("http://www.opensilex.org/vocabulary/oeso"), ontologyStream, Lang.RDFXML);
        ontologyStream.close();

        LOGGER.info("Install oeev ontology: http://www.opensilex.org/vocabulary/oeev");
        ontologyStream = new FileInputStream(ClassUtils.getFileFromClassArtifact(SPARQLModule.class, "install/oeev.owl"));
        sparql.loadOntologyStream(new URI("http://www.opensilex.org/vocabulary/oeev"), ontologyStream, Lang.RDFXML);
        ontologyStream.close();
        
        SPARQLConfig sparqlConfig = OpenSilex.getModuleConfig(SPARQLModule.class, SPARQLConfig.class);
        URI graph = new URI(sparqlConfig.baseURI() + "species");
        
        LOGGER.info("Install Agrovoc species: " + graph.toString());
        ontologyStream = new FileInputStream(ClassUtils.getFileFromClassArtifact(SPARQLModule.class, "install/species.ttl"));
        sparql.loadOntologyStream(graph, ontologyStream, Lang.TTL);
        ontologyStream.close();
        
        factory.dispose(sparql);
    }
    
    private static void createRDF4JRepository(RDF4JConfig config, boolean reset) throws IOException {
        RepositoryManager repositoryManager = RepositoryProvider.getRepositoryManager(config.serverURI());
        repositoryManager.init();

        // Remove existing repository
        if (reset) {
            repositoryManager.removeRepository(config.repository());
        }

        // Read repository configuration file located in jar
        File rdf4jApiJar = ClassUtils.getJarFile(RepositoryConfig.class);
        File templateFile = ClassUtils.getFileFromJar(rdf4jApiJar, "org/eclipse/rdf4j/repository/config/native-shacl.ttl");
        InputStream templateStream = new FileInputStream(templateFile);
        String template;
        try {
            template = IOUtil.readString(new InputStreamReader(templateStream, "UTF-8"));
        } finally {
            templateStream.close();
        }
        final ConfigTemplate configTemplate = new ConfigTemplate(template);

        // This variable contains all keys parsed from template, use debugger to watch them.
        // final Map<String, List<String>> variableMap = configTemplate.getVariableMap();
        // Define repository template parameters
        final Map<String, String> valueMap = new HashMap<String, String>() {
            {
                put("Repository ID", config.repository());
                put("Repository title", config.repository());
                // Default template value write here for information
                 put("Query Iteration Cache size", "10000");
                 put("Triple indexes", "spoc,posc");
                // put("EvaluationStrategyFactory", "org.eclipse.rdf4j.query.algebra.evaluation.impl.StrictEvaluationStrategyFactory");

            }
        };

        final String configString = configTemplate.render(valueMap);
        final Model graph = new LinkedHashModel();

        final RDFParser rdfParser = Rio.createParser(RDFFormat.TURTLE, SimpleValueFactory.getInstance());
        rdfParser.setRDFHandler(new StatementCollector(graph));
        rdfParser.parse(new StringReader(configString), RepositoryConfigSchema.NAMESPACE);

        final org.eclipse.rdf4j.model.Resource repositoryNode = Models
                .subject(graph.filter(null, RDF.TYPE, RepositoryConfigSchema.REPOSITORY))
                .orElseThrow(() -> new RepositoryConfigException("missing repository node"));

        RepositoryRegistry registry = RepositoryRegistry.getInstance();
        ServiceLoader<RepositoryFactory> services = ServiceLoader.load(RepositoryFactory.class, OpenSilex.getClassLoader());
        services.forEach(action -> {
            registry.add(action);
        });

        final RepositoryConfig repConfig = RepositoryConfig.create(graph, repositoryNode);
        repConfig.validate();

        repositoryManager.addRepositoryConfig(repConfig);
        repositoryManager.shutDown();
    }
}

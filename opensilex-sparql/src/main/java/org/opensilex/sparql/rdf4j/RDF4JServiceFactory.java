/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql.rdf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.pool.PoolStats;
import org.eclipse.rdf4j.common.io.IOUtil;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.config.ConfigTemplate;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.repository.config.RepositoryConfigSchema;
import org.eclipse.rdf4j.repository.config.RepositoryFactory;
import org.eclipse.rdf4j.repository.config.RepositoryRegistry;
import org.eclipse.rdf4j.repository.http.HTTPRepository;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;
import org.eclipse.rdf4j.repository.manager.RepositoryProvider;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.opensilex.OpenSilex;
import org.opensilex.service.ServiceDefaultDefinition;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.service.SPARQLServiceFactory;
import org.opensilex.utils.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author vince
 */
@ServiceDefaultDefinition(config = RDF4JConfig.class)
public class RDF4JServiceFactory extends SPARQLServiceFactory {

    private final static Logger LOGGER = LoggerFactory.getLogger(RDF4JServiceFactory.class);

    private final Repository repository;
    private PoolingHttpClientConnectionManager cm;

    public RDF4JServiceFactory(RDF4JConfig config) {
        super(config);
        LOGGER.debug("Build RDF4JServiceFactory from config");
        synchronized (this) {
            HTTPRepository repo = new HTTPRepository(config.serverURI(), config.repository());
            cm = new PoolingHttpClientConnectionManager();
            cm.setDefaultMaxPerRoute(20);
            CloseableHttpClient httpClient = HttpClients.custom()
                    .setConnectionManager(cm)
                    .build();

            repo.setHttpClient(httpClient);
            repo.init();
            this.repository = repo;
        }

    }

    public RDF4JServiceFactory(Repository repository) {
        super(null);
        LOGGER.debug("Build RDF4JServiceFactory from repository");
        synchronized (this) {
            this.repository = repository;
            this.repository.init();
        }
    }

    public RDF4JConfig getImplementedConfig() {
        return (RDF4JConfig) getConfig();
    }

    private int getTimeout() {
        if (getConfig() == null) {
            return 0;
        }

        return getImplementedConfig().timeout();
    }

    protected synchronized SPARQLService getNewService() throws Exception {
        RepositoryConnection connection = repository.getConnection();
        if (cm != null && LOGGER.isDebugEnabled()) {
            PoolStats stats = cm.getTotalStats();
            LOGGER.debug(
                    "Connection pool stats: \n"
                    + "In use    -> " + stats.getLeased() + "\n"
                    + "Pending   -> " + stats.getPending() + "\n"
                    + "Available -> " + stats.getAvailable() + "\n"
                    + "Max       -> " + stats.getMax() + "\n"
            );
        }

        RDF4JConnection rdf4jConnection = new RDF4JConnection(connection);
        rdf4jConnection.setTimeout(getTimeout());
        SPARQLService sparql = new SPARQLService(rdf4jConnection);
        sparql.setOpenSilex(getOpenSilex());
        sparql.setMapperIndex(getMapperIndex());
        sparql.setDefaultLang(getDefaultLanguage());
        sparql.setup();
        return sparql;
    }

    private synchronized void closeService(SPARQLService sparql) throws Exception {
        if (sparql != null) {
            sparql.shutdown();
        }
    }

    @Override
    public SPARQLService provide() {
        try {
            return getNewService();
        } catch (Exception ex) {
            LOGGER.error("Error while opening RDF4J service connection instance", ex);
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void dispose(SPARQLService sparql) {
        try {
            closeService(sparql);
        } catch (Exception ex) {
            LOGGER.error("Error while closing RDF4J service connectioninstance instance", ex);
        }

    }

    @Override
    public void createRepository() throws Exception {
        if (getImplementedConfig() != null) {
            // Create repository
            RepositoryManager repositoryManager = RepositoryProvider.getRepositoryManager(getImplementedConfig().serverURI());
            repositoryManager.init();

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
                    put("Repository ID", getImplementedConfig().repository());
                    put("Repository title", getImplementedConfig().repository());
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

    @Override
    public void deleteRepository() throws Exception {
        if (getImplementedConfig() != null) {
            // Create repository
            RepositoryManager repositoryManager = RepositoryProvider.getRepositoryManager(getImplementedConfig().serverURI());
            repositoryManager.init();
            repositoryManager.removeRepository(getImplementedConfig().repository());
            repositoryManager.shutDown();
        }
    }

}

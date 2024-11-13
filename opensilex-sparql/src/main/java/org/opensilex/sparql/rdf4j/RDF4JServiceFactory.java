/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.sparql.rdf4j;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.pool.PoolStats;
import org.eclipse.rdf4j.common.io.IOUtil;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.CONFIG;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.config.ConfigTemplate;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.repository.config.RepositoryFactory;
import org.eclipse.rdf4j.repository.config.RepositoryRegistry;
import org.eclipse.rdf4j.repository.http.HTTPRepository;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;
import org.eclipse.rdf4j.repository.manager.RepositoryProvider;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.eclipse.rdf4j.sail.config.SailFactory;
import org.eclipse.rdf4j.sail.config.SailRegistry;
import org.opensilex.OpenSilex;
import org.opensilex.service.ServiceDefaultDefinition;
import org.opensilex.sparql.SPARQLConfig;
import org.opensilex.sparql.deserializer.URIDeserializer;
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

    @Override
    public void startup() throws Exception {
        super.startup();

        SPARQLConfig sparqlConfig = sparqlModule.getConfig(SPARQLConfig.class);

        // Add the prefixes defined only in the triple store
        if (sparqlConfig.usePrefixes()) {
            try (RepositoryConnection connection = this.repository.getConnection();
                 RepositoryResult<Namespace> result = connection.getNamespaces()) {
                for (Namespace ns : result) {
                    SPARQLService.addPrefix(ns.getPrefix(), ns.getName());
                }
                URIDeserializer.setPrefixes(SPARQLService.getPrefixMapping(), sparqlConfig.usePrefixes());
            } catch (RepositoryException ignored) {
                // No repository connection to establish. That is the case for example during Swagger generation.
            }
        }
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

    /**
     * @return the rdf4j repository creation template file
     * @throws IOException if the file can't be read
     */
    protected File getRepositoryCreationTemplateFile() throws IOException {
        // Read repository configuration file located in jar
        File rdf4jApiJar = ClassUtils.getJarFile(RepositoryConfig.class);
        return ClassUtils.getFileFromJar(rdf4jApiJar, "org/eclipse/rdf4j/repository/config/native-shacl.ttl");
    }

    /**
     *
     * @return a non-null {@link Map} which contains all repository custom properties and properties value.
     * These settings override settings already present in the repository template file returned by {@link #getRepositoryCreationTemplateFile()}
     * @see <a href="https://rdf4j.org/documentation/tools/repository-configuration/"> RDF4J repository configuration </a>
     * @see <a href="https://rdf4j.org/documentation/reference/configuration/"> RDF4J repository configuration </a>
     */
    protected Map<String,String> getCustomRepositorySettings(){
        Map<String,String> settings = new HashMap<>();

        // Go to https://rdf4j.org/documentation/reference/configuration/ and search 'Native store indexes' for more details
        settings.put(TRIPLE_INDEXES_PARAMETER, "spoc,posc,opsc");

        return settings;
    }

    /** name of the template used in rdf4j to define the repository id */
    protected static final String REPOSITORY_ID_TEMPLATE_PARAMETER = "Repository ID";

    /** name of the template used in rdf4j to define the repository title */
    protected static final String REPOSITORY_TITLE_TEMPLATE_PARAMETER = "Repository title";

    // define default indexes used during query evaluation
    protected static final String TRIPLE_INDEXES_PARAMETER = "Triple indexes";

    @Override
    public void createRepository() throws Exception {
        if (getImplementedConfig() != null) {
            // Create repository
            RepositoryManager repositoryManager = RepositoryProvider.getRepositoryManager(getImplementedConfig().serverURI());
            repositoryManager.init();

            // read the repository template file
            File templateFile = getRepositoryCreationTemplateFile();
            InputStream templateStream = new FileInputStream(templateFile);
            String template;
            try {
                template = IOUtil.readString(new InputStreamReader(templateStream, StandardCharsets.UTF_8));
            } finally {
                templateStream.close();
            }
            final ConfigTemplate configTemplate = new ConfigTemplate(template);

            final Map<String, String> repositorySettings = new HashMap<>();

            // define repository id and title
            repositorySettings.put(REPOSITORY_ID_TEMPLATE_PARAMETER, getImplementedConfig().repository());
            repositorySettings.put(REPOSITORY_TITLE_TEMPLATE_PARAMETER, getImplementedConfig().repository());

            // define settings for the RDF4J service implementation
            repositorySettings.putAll(getCustomRepositorySettings());

            final String configString = configTemplate.render(repositorySettings);
            final Model graph = new LinkedHashModel();

            final RDFParser rdfParser = Rio.createParser(RDFFormat.TURTLE, SimpleValueFactory.getInstance());
            rdfParser.setRDFHandler(new StatementCollector(graph));
            rdfParser.parse(new StringReader(configString), CONFIG.NAMESPACE);

            final org.eclipse.rdf4j.model.Resource repositoryNode = Models
                    .subject(graph.filter(null, RDF.TYPE, CONFIG.Rep.Repository))
                    .orElseThrow(() -> new RepositoryConfigException("missing repository node"));

            loadRDF4JServices();

            final RepositoryConfig repConfig = RepositoryConfig.create(graph, repositoryNode);
            repConfig.validate();

            repositoryManager.addRepositoryConfig(repConfig);
            repositoryManager.shutDown();
            LOGGER.info("Repository created {}@{} [OK]", getImplementedConfig().serverURI(), getImplementedConfig().repository());
        }
    }

    /**
     * Load all {@link RepositoryRegistry} and {@link SailRegistry} to ensure that repository creation will recognize all available services
     */
    private void loadRDF4JServices(){

        // Load all RepositoryRegistry with ServiceLoader, needed to ensure that all RepositoryRegistry are available for repository creation
        RepositoryRegistry repositoryRegistry = RepositoryRegistry.getInstance();
        ServiceLoader<RepositoryFactory> services = ServiceLoader.load(RepositoryFactory.class, OpenSilex.getClassLoader());
        services.forEach(repositoryRegistry::add);

        repositoryRegistry.getAll().forEach(registry -> LOGGER.info("RepositoryRegistry : {}", registry.getRepositoryType()));

        // Load all SailRegistry with ServiceLoader, needed to ensure that all SailRegistry are available for repository sail delegation
        SailRegistry sailRegistry = SailRegistry.getInstance();
        ServiceLoader<SailFactory> sails = ServiceLoader.load(SailFactory.class, OpenSilex.getClassLoader());
        sails.forEach(sailRegistry::add);

        sailRegistry.getAll().forEach(registry -> LOGGER.info("SailFactory : {}", registry.getSailType()));
    }

    @Override
    public void deleteRepository() throws Exception {
        if (getImplementedConfig() != null) {
            // Create repository
            RepositoryManager repositoryManager = RepositoryProvider.getRepositoryManager(getImplementedConfig().serverURI());
            repositoryManager.init();
            repositoryManager.removeRepository(getImplementedConfig().repository());
            repositoryManager.shutDown();
            LOGGER.info("Repository deleted {}@{} [OK]", getImplementedConfig().serverURI(), getImplementedConfig().repository());
        }
    }

    public final Repository getRepository() {
        return repository;
    }
}

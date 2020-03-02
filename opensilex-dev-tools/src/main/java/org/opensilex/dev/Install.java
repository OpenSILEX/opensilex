//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.dev;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoDatabase;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.net.URI;
import java.util.ServiceLoader;
import javax.mail.internet.InternetAddress;
import opensilex.service.PhisPostgreSQLConfig;
import opensilex.service.PhisWsConfig;
import opensilex.service.PhisWsModule;
import org.apache.jena.riot.Lang;
import org.bson.Document;
import org.eclipse.rdf4j.common.io.IOUtil;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
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
import org.opensilex.nosql.mongodb.MongoDBConfig;
import org.opensilex.rest.authentication.AuthenticationService;
import org.opensilex.rest.user.dal.UserDAO;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.rdf4j.RDF4JConfig;
import org.opensilex.utils.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author vincent
 */
public class Install {

    private final static Logger LOGGER = LoggerFactory.getLogger(OpenSilex.class);

    private static OpenSilex opensilex;

    private static boolean deleteFirst = false;

    public static void main(String[] args) throws Exception {
        install(false, null);
    }

    public static void install(boolean deleteFirst, String baseDirectory) throws Exception {
        Install.deleteFirst = deleteFirst;

        Map<String, String> args = new HashMap<String, String>() {
            {
                put(OpenSilex.PROFILE_ID_ARG_KEY, OpenSilex.DEV_PROFILE_ID);
            }
        };

        if (baseDirectory != null) {
            args.put(OpenSilex.BASE_DIR_ARG_KEY, baseDirectory);
            args.put(OpenSilex.CONFIG_FILE_ARG_KEY, getConfig(baseDirectory));
        } else {
            args.put(OpenSilex.CONFIG_FILE_ARG_KEY, getConfig(System.getProperty("user.dir")));
        }

        OpenSilex.setup(args);

        opensilex = OpenSilex.getInstance();

//        LOGGER.info("Initialize PostGreSQL");
//        initPGSQL();

        LOGGER.info("Initialize RDF4J");
        initRDF4J();

        LOGGER.info("Initialize MongoDB");
        initMongo();

        LOGGER.info("Initialize Modules");
        opensilex.install();

        LOGGER.info("Create Super Admin");
        createSuperAdmin();
    }

    private static String getConfig(String baseDirectory) {
        return Paths.get(baseDirectory).resolve(DevModule.CONFIG_FILE_PATH).toFile().getAbsolutePath();
    }

    private static File getResourceFile(String path) {
        return OpenSilex.getInstance().getBaseDirectory().resolve("./src/main/resources/").resolve(path).toFile();
    }

    private static Connection getDBConnection(PhisPostgreSQLConfig pgConfig, String dbId) throws Exception {
        Class.forName("org.postgresql.Driver");
        String dbUri = "jdbc:postgresql://" + pgConfig.host()
                + ":" + pgConfig.port() + "/" + dbId;

        return DriverManager.getConnection(dbUri, pgConfig.username(), pgConfig.password());
    }

    private static void initPGSQL() throws Exception {
        PhisWsModule phis = opensilex.getModuleByClass(PhisWsModule.class);
        PhisWsConfig phisConfig = phis.getConfig(PhisWsConfig.class);
        PhisPostgreSQLConfig pgConfig = phisConfig.postgreSQL();

        Connection connection = null;
        Statement statement = null;

        try {

            if (deleteFirst) {
                connection = getDBConnection(pgConfig, "postgres");
                statement = connection.createStatement();
                statement.execute("SELECT pg_terminate_backend(pg_stat_activity.pid)\n"
                        + "FROM pg_stat_activity\n"
                        + "WHERE pg_stat_activity.datname = '" + pgConfig.database() + "'\n"
                        + "  AND pid <> pg_backend_pid();");
                statement.executeUpdate("DROP DATABASE IF EXISTS " + pgConfig.database());
                statement.executeUpdate("CREATE DATABASE " + pgConfig.database());
                statement.close();
                connection.close();
            }

            connection = getDBConnection(pgConfig, pgConfig.database());
            statement = connection.createStatement();

            // initialize file reader
            BufferedReader reader = new BufferedReader(new FileReader(getResourceFile("./install/opensilex_st_dump.sql")));
            String line = null;
            String statementValue = "";
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("--")) {
                    LOGGER.debug("Ignore comment: " + line);
                } else if (line.endsWith(";")) {
                    statementValue += "\n" + line;
                    LOGGER.debug("Execute statement: " + statementValue);
                    statement.execute(statementValue);
                    statementValue = "";
                } else if (!line.isEmpty()) {
                    if (statementValue.isEmpty()) {
                        statementValue = line;
                    } else {
                        statementValue += "\n" + line;
                    }
                }
            }
        } catch (Exception ex) {
            if (statement != null && !statement.isClosed()) {
                statement.close();
            }
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }

            throw ex;
        }

    }

    private static void initRDF4J() throws Exception {
        // initialise repository manager
        RDF4JConfig config = opensilex.loadConfigPath("ontologies.sparql.rdf4j", RDF4JConfig.class);

        // Create repository
        createRDF4JRepository(config);

        // Restart repository to reload sparql service
        opensilex.restart();
        SPARQLService sparql = opensilex.getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLService.class);

        // Import default ontologies
        InputStream ontologyStream = new FileInputStream(getResourceFile("./install/oa.rdf"));
        sparql.loadOntologyStream(new URI("http://www.w3.org/ns/oa"), ontologyStream, Lang.RDFXML);
        ontologyStream.close();

        ontologyStream = new FileInputStream(getResourceFile("./install/oeso.owl"));
        sparql.loadOntologyStream(new URI("http://www.opensilex.org/vocabulary/oeso"), ontologyStream, Lang.RDFXML);
        ontologyStream.close();

        ontologyStream = new FileInputStream(getResourceFile("./install/oeev.owl"));
        sparql.loadOntologyStream(new URI("http://www.opensilex.org/vocabulary/oeev"), ontologyStream, Lang.RDFXML);
        ontologyStream.close();
    }

    private static void createRDF4JRepository(RDF4JConfig config) throws IOException {
        RepositoryManager repositoryManager = RepositoryProvider.getRepositoryManager(config.serverURI());
        repositoryManager.init();

        // Remove existing repository
        if (deleteFirst) {
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
                // put("Query Iteration Cache size", "10000");
                // put("Triple indexes", "spoc,posc");
                // put("EvaluationStrategyFactory", "org.eclipse.rdf4j.query.algebra.evaluation.impl.StrictEvaluationStrategyFactory");

            }
        };

        final String configString = configTemplate.render(valueMap);
        final Model graph = new LinkedHashModel();

        final RDFParser rdfParser = Rio.createParser(RDFFormat.TURTLE, SimpleValueFactory.getInstance());
        rdfParser.setRDFHandler(new StatementCollector(graph));
        rdfParser.parse(new StringReader(configString), RepositoryConfigSchema.NAMESPACE);

        final Resource repositoryNode = Models
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

    private static void initMongo() throws Exception {
        MongoDBConfig config = opensilex.loadConfigPath("big-data.nosql.mongodb", MongoDBConfig.class);

        String host = config.host();
        int port = config.port();
        String user = config.username();
        String password = config.password();
        String authdb = config.authDB();
        String url = "mongodb://";

        if (!user.equals("") && !password.equals("")) {
            url += user + ":" + password + "@";
        }

        url += host + ":" + port + "/";

        if (!authdb.equals("")) {
            url += "?authSource=" + authdb;
        }

        MongoClient mongo = new MongoClient(new MongoClientURI(url));

        MongoDatabase adminDb = mongo.getDatabase("admin");
        if (!deleteFirst) {
            adminDb.runCommand(new Document("replSetInitiate", new Document()));
        } else {
            mongo.dropDatabase(config.database());
        }
    }

    private static void createSuperAdmin() throws Exception {
        SPARQLService sparql = opensilex.getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLService.class);
        AuthenticationService authentication = opensilex.getServiceInstance(AuthenticationService.DEFAULT_AUTHENTICATION_SERVICE, AuthenticationService.class);

        UserDAO userDAO = new UserDAO(sparql);

        InternetAddress email = new InternetAddress("admin@opensilex.org");
        userDAO.create(null, email, "Admin", "OpenSilex", true, authentication.getPasswordHash("admin"), "en-US");
    }
}

//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.dev;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.net.URI;
import javax.mail.internet.InternetAddress;
import opensilex.service.PhisPostgreSQLConfig;
import opensilex.service.PhisWsConfig;
import opensilex.service.PhisWsModule;
import org.apache.jena.riot.Lang;
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
import org.eclipse.rdf4j.repository.manager.RepositoryManager;
import org.eclipse.rdf4j.repository.manager.RepositoryProvider;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.opensilex.OpenSilex;
import org.opensilex.server.security.AuthenticationService;
import org.opensilex.server.user.dal.UserDAO;
import org.opensilex.sparql.SPARQLService;
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

    public static void main(String[] args) throws Exception {

        String configFile = getResourceFile("./config/opensilex.yml").getCanonicalPath();
        OpenSilex.setup(new HashMap<String, String>() {
            {
                put(OpenSilex.PROFILE_ID_ARG_KEY, OpenSilex.DEV_PROFILE_ID);
                put(OpenSilex.CONFIG_FILE_ARG_KEY, configFile);
                put(OpenSilex.DEBUG_ARG_KEY, "true");
            }
        });

        opensilex = OpenSilex.getInstance();

//        initPGSQL();
        initRDF4J();

//        opensilex.install();
        createSuperAdmin();
    }

    private static File getResourceFile(String path) {
        Path currentDirectory = Paths.get(System.getProperty("user.dir"));
        return currentDirectory.resolve("./src/main/resources/").resolve(path).toFile();
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
            connection = getDBConnection(pgConfig, "postgres");
            statement = connection.createStatement();

            statement.executeUpdate("DROP DATABASE IF EXISTS " + pgConfig.database());
            statement.executeUpdate("CREATE DATABASE " + pgConfig.database());
            statement.close();
            connection.close();

            connection = getDBConnection(pgConfig, pgConfig.database());
            statement = connection.createStatement();

            // initialize file reader
            BufferedReader reader = new BufferedReader(new FileReader(getResourceFile("./docker/opensilex_st_dump.sql")));
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
        RDF4JConfig config = opensilex.loadConfigPath("opensilex.sparql.rdf4j", RDF4JConfig.class);

        // Create repository
        createRDF4JRepository(config);

        // Reset repository to reload sparql service
        opensilex.reset();
        SPARQLService sparql = opensilex.getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLService.class);

        // Import default ontologies
        InputStream ontologyStream = new FileInputStream(getResourceFile("./docker/oa.rdf"));
        sparql.loadOntologyStream(new URI("http://www.w3.org/ns/oa"), ontologyStream, Lang.RDFXML);
        ontologyStream.close();

        ontologyStream = new FileInputStream(getResourceFile("./docker/oeso.owl"));
        sparql.loadOntologyStream(new URI("http://www.opensilex.org/vocabulary/oeso"), ontologyStream, Lang.RDFXML);
        ontologyStream.close();

        ontologyStream = new FileInputStream(getResourceFile("./docker/oeev.owl"));
        sparql.loadOntologyStream(new URI("http://www.opensilex.org/vocabulary/oeev"), ontologyStream, Lang.RDFXML);
        ontologyStream.close();
    }

    private static void createRDF4JRepository(RDF4JConfig config) throws IOException {
        RepositoryManager repositoryManager = RepositoryProvider.getRepositoryManager(config.serverURI());
        repositoryManager.init();

        // Remove existing repository
        repositoryManager.removeRepository(config.repository());

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

        final RepositoryConfig repConfig = RepositoryConfig.create(graph, repositoryNode);
        repConfig.validate();

        repositoryManager.addRepositoryConfig(repConfig);
        repositoryManager.shutDown();
    }

    private static void createSuperAdmin() throws Exception {
        SPARQLService sparql = opensilex.getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLService.class);
        AuthenticationService authentication = opensilex.getServiceInstance(AuthenticationService.DEFAULT_AUTHENTICATION_SERVICE, AuthenticationService.class);

        UserDAO userDAO = new UserDAO(sparql, authentication);

        InternetAddress email = new InternetAddress("admin@opensilex.org");
        userDAO.create(email, "Admin", "OpenSilex", true, "admin");
    }
}

//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.dev;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import javax.mail.internet.InternetAddress;
import org.opensilex.OpenSilex;
import org.opensilex.rest.authentication.AuthenticationService;
import org.opensilex.rest.user.dal.UserDAO;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.service.SPARQLServiceFactory;
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
        install(false, null);
    }

    public static void install(boolean deleteFirst, String baseDirectory) throws Exception {

        Map<String, String> args = new HashMap<String, String>() {
            {
                put(OpenSilex.PROFILE_ID_ARG_KEY, OpenSilex.DEV_PROFILE_ID);
//                put(OpenSilex.DEBUG_ARG_KEY, "true");
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
        LOGGER.info("Initialize Modules");
        opensilex.install(deleteFirst);

        LOGGER.info("Create Super Admin");
        createSuperAdmin();
    }

    private static String getConfig(String baseDirectory) {
        return Paths.get(baseDirectory).resolve(DevModule.CONFIG_FILE_PATH).toFile().getAbsolutePath();
    }

//    private static File getResourceFile(String path) {
//        return OpenSilex.getInstance().getBaseDirectory().resolve("./src/main/resources/").resolve(path).toFile();
//    }
//
//    private static Connection getDBConnection(PhisPostgreSQLConfig pgConfig, String dbId) throws Exception {
//        Class.forName("org.postgresql.Driver");
//        String dbUri = "jdbc:postgresql://" + pgConfig.host()
//                + ":" + pgConfig.port() + "/" + dbId;
//
//        return DriverManager.getConnection(dbUri, pgConfig.username(), pgConfig.password());
//    }
//    private static void initPGSQL() throws Exception {
//        PhisWsModule phis = opensilex.getModuleByClass(PhisWsModule.class);
//        PhisWsConfig phisConfig = phis.getConfig(PhisWsConfig.class);
//        PhisPostgreSQLConfig pgConfig = phisConfig.postgreSQL();
//
//        Connection connection = null;
//        Statement statement = null;
//
//        try {
//
//            if (deleteFirst) {
//                connection = getDBConnection(pgConfig, "postgres");
//                statement = connection.createStatement();
//                statement.execute("SELECT pg_terminate_backend(pg_stat_activity.pid)\n"
//                        + "FROM pg_stat_activity\n"
//                        + "WHERE pg_stat_activity.datname = '" + pgConfig.database() + "'\n"
//                        + "  AND pid <> pg_backend_pid();");
//                statement.executeUpdate("DROP DATABASE IF EXISTS " + pgConfig.database());
//                statement.executeUpdate("CREATE DATABASE " + pgConfig.database());
//                statement.close();
//                connection.close();
//            }
//
//            connection = getDBConnection(pgConfig, pgConfig.database());
//            statement = connection.createStatement();
//
//            // initialize file reader
//            BufferedReader reader = new BufferedReader(new FileReader(getResourceFile("./install/opensilex_st_dump.sql")));
//            String line = null;
//            String statementValue = "";
//            while ((line = reader.readLine()) != null) {
//                if (line.startsWith("--")) {
//                    LOGGER.debug("Ignore comment: " + line);
//                } else if (line.endsWith(";")) {
//                    statementValue += "\n" + line;
//                    LOGGER.debug("Execute statement: " + statementValue);
//                    statement.execute(statementValue);
//                    statementValue = "";
//                } else if (!line.isEmpty()) {
//                    if (statementValue.isEmpty()) {
//                        statementValue = line;
//                    } else {
//                        statementValue += "\n" + line;
//                    }
//                }
//            }
//        } catch (Exception ex) {
//            if (statement != null && !statement.isClosed()) {
//                statement.close();
//            }
//            if (connection != null && !connection.isClosed()) {
//                connection.close();
//            }
//
//            throw ex;
//        }
//
//    }
    private static void createSuperAdmin() throws Exception {
        SPARQLService sparql = opensilex.getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class).provide();
        try {
            AuthenticationService authentication = opensilex.getServiceInstance(AuthenticationService.DEFAULT_AUTHENTICATION_SERVICE, AuthenticationService.class);

            UserDAO userDAO = new UserDAO(sparql);

            InternetAddress email = new InternetAddress("admin@opensilex.org");
            userDAO.create(null, email, "Admin", "OpenSilex", true, authentication.getPasswordHash("admin"), "en-US");
        } finally {
            sparql.shutdown();
        }
    }
}

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

        LOGGER.info("Initialize Modules");
        opensilex.install(deleteFirst);

        LOGGER.info("Create Super Admin");
        createSuperAdmin();
    }

    private static String getConfig(String baseDirectory) {
        return Paths.get(baseDirectory).resolve(DevModule.CONFIG_FILE_PATH).toFile().getAbsolutePath();
    }

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

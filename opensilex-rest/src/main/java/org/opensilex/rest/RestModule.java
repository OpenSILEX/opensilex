//******************************************************************************
//                          RestModule.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.rest;

import java.net.URI;
import org.opensilex.rest.authentication.SecurityOntology;
import org.opensilex.rest.extensions.APIExtension;
import java.util.*;
import javax.mail.internet.InternetAddress;
import org.opensilex.OpenSilex;
import org.opensilex.module.ModuleConfig;
import org.opensilex.OpenSilexModule;
import org.opensilex.rest.authentication.AuthenticationService;
import org.opensilex.rest.profile.dal.ProfileModel;
import org.opensilex.rest.security.dal.SecurityAccessDAO;
import org.opensilex.rest.user.dal.UserDAO;
import org.opensilex.sparql.rdf4j.RDF4JInMemoryServiceFactory;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.service.SPARQLServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <pre>
 * Base module implementation for OpenSilex.
 * - Enable Swagger
 * - Enable Security web services
 * - Enable SPARQL service through configuration
 * - Enable Big Data service through configuration
 * - Enable File System service through configuration
 * - Enable Authentication service through configuration
 * </pre>
 *
 * @see org.opensilex.server.ServerConfig
 * @author Vincent Migot
 */
public class RestModule extends OpenSilexModule implements APIExtension {

    public final static String REST_SECURITY_API_ID = "Security";

    public final static String REST_SECURITY_CONCEPTS_API_ID = "Users, Groups & Profiles";

    private static final Logger LOGGER = LoggerFactory.getLogger(RestModule.class);

    @Override
    public Class<? extends ModuleConfig> getConfigClass() {
        return RestConfig.class;
    }

    @Override
    public String getConfigId() {
        return "rest";
    }

    @Override
    public List<String> getPackagesToScan() {
        List<String> list = APIExtension.super.getPackagesToScan();
        list.add("io.swagger.jaxrs.listing");
        list.add("org.opensilex.rest.authentication");
        list.add("org.opensilex.rest.filters");
        list.add("org.opensilex.rest.validation");

        return list;
    }

    @Override
    public void startup() throws Exception {
        SPARQLService.addPrefix(SecurityOntology.PREFIX, SecurityOntology.NAMESPACE);
        SPARQLServiceFactory factory = OpenSilex.getInstance().getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class);
        if (factory instanceof RDF4JInMemoryServiceFactory) {
            RestModule.createDefaultSuperAdmin();
        }
    }

    @Override
    public void install(boolean reset) throws Exception {
        LOGGER.info("Create default profile");
        createDefaultProfile(reset);
    }

    private final static String DEFAULT_PROFILE_URI = "http://www.opensilex.org/profiles/default-profile";
    private final static String DEFAULT_PROFILE_NAME = "Default profile";

    public static void createDefaultProfile(boolean reset) throws Exception {
        SPARQLServiceFactory factory = OpenSilex.getInstance().getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class);
        SPARQLService sparql = factory.provide();

        SecurityAccessDAO securityDAO = new SecurityAccessDAO(sparql);

        ProfileModel profile = new ProfileModel();
        profile.setUri(new URI(DEFAULT_PROFILE_URI));
        profile.setName(DEFAULT_PROFILE_NAME);
        profile.setCredentials(securityDAO.getCredentialsIdList());
        sparql.create(profile);
        factory.dispose(sparql);
    }

    @Override
    public void check() throws Exception {
        LOGGER.info("Check User existence");
        SPARQLServiceFactory factory = OpenSilex.getInstance().getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class);
        SPARQLService sparql = factory.provide();
        UserDAO userDAO = new UserDAO(sparql);
        int userCount = userDAO.getCount();
        factory.dispose(sparql);
        if (userCount == 0) {
            LOGGER.warn("/!\\ Caution, you don't have any user registered in OpenSilex");
            LOGGER.warn("/!\\ You probably should add one with command `opensilex user add ...` (use --help flag for more information)");
        }
    }

    public static void createDefaultSuperAdmin() throws Exception {
        OpenSilex opensilex = OpenSilex.getInstance();
        SPARQLService sparql = opensilex.getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class).provide();
        try {
            AuthenticationService authentication = opensilex.getServiceInstance(AuthenticationService.DEFAULT_AUTHENTICATION_SERVICE, AuthenticationService.class);

            createDefaultSuperAdmin(sparql, authentication);
        } finally {
            sparql.shutdown();
        }
    }

    public static void createDefaultSuperAdmin(SPARQLService sparql, AuthenticationService authentication) throws Exception {
        UserDAO userDAO = new UserDAO(sparql);
        InternetAddress email = new InternetAddress("admin@opensilex.org");

        if (!userDAO.userEmailexists(email)) {
            userDAO.create(null, email, "Admin", "OpenSilex", true, authentication.getPasswordHash("admin"), "en-US");
        }
    }
}

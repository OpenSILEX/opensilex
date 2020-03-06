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
import org.opensilex.OpenSilex;
import org.opensilex.module.ModuleConfig;
import org.opensilex.OpenSilexModule;
import org.opensilex.rest.profile.dal.ProfileModel;
import org.opensilex.rest.security.dal.SecurityAccessDAO;
import org.opensilex.rest.user.dal.UserDAO;
import org.opensilex.rest.user.dal.UserModel;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.service.SPARQLServiceFactory;
import org.opensilex.utils.ListWithPagination;

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
        ListWithPagination<UserModel> result = userDAO.search(null, null, null, null);
        factory.dispose(sparql);
        if (result.getTotal() == 0) {
            LOGGER.error("You should at least have one user defined to have a valid configuration");
            throw new Exception();
        }
    }


}

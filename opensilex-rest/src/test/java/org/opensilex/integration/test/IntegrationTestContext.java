//******************************************************************************
//                          IntegrationTestContext.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.integration.test;

import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.mockito.Mockito;
import org.opensilex.OpenSilex;
import org.opensilex.rest.RestApplication;
import org.opensilex.rest.authentication.AuthenticationService;
import org.opensilex.rest.user.dal.UserDAO;
import org.opensilex.sparql.SPARQLModule;
import org.opensilex.sparql.exceptions.SPARQLQueryException;
import org.opensilex.sparql.service.SPARQLService;

import javax.mail.internet.InternetAddress;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opensilex.sparql.service.SPARQLServiceFactory;

/**
 * @author Renaud COLIN
 *
 * An utility class used in order to init an {@link OpenSilex} instance for unit
 * and integration testing.
 */
public class IntegrationTestContext {

    private ResourceConfig resourceConfig;

    public IntegrationTestContext(boolean debug) throws Exception {

        Map<String, String> args = new HashMap<>();
        args.put(OpenSilex.PROFILE_ID_ARG_KEY, OpenSilex.TEST_PROFILE_ID);

        if (debug) {
            args.put(OpenSilex.DEBUG_ARG_KEY, "true");
        }

        // initialize application
        OpenSilex.setup(args);

        OpenSilex opensilex = OpenSilex.getInstance();

        resourceConfig = new RestApplication(OpenSilex.getInstance());

        // create a mock for HttpServletRequest which is not available with grizzly
        final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        resourceConfig.register(new AbstractBinder() {
            @Override
            protected void configure() {
                bind(request).to(HttpServletRequest.class);
            }
        });

        // add the admin user
        try (SPARQLService sparqlService = getSparqlService()) {
            AuthenticationService authentication = opensilex.getServiceInstance(AuthenticationService.DEFAULT_AUTHENTICATION_SERVICE, AuthenticationService.class);
            UserDAO userDAO = new UserDAO(sparqlService);
            InternetAddress email = new InternetAddress("admin@opensilex.org");
            userDAO.create(null, email, "Admin", "OpenSilex", true, authentication.getPasswordHash("admin"), "en-US");
        }
    }

    public ResourceConfig getResourceConfig() {
        return resourceConfig;
    }

    /**
     *
     * @return the {@link SPARQLService} used for tests
     */
    public SPARQLService getSparqlService() {
        return OpenSilex.getInstance().getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class).provide();
    }

    /**
     *
     * @return the {@link AuthenticationService} used for tests
     */
    public AuthenticationService getAuthenticationService() {
        return OpenSilex.getInstance().getServiceInstance(AuthenticationService.DEFAULT_AUTHENTICATION_SERVICE, AuthenticationService.class);
    }

    /**
     * Clear the list of SPARQL graph to clear after each test execution
     *
     * @throws SPARQLQueryException if an errors occurs during SPARQL query
     * execution
     */
    public void clearGraphs(List<String> graphsToClear) throws Exception {
        try (SPARQLService sparqlService = getSparqlService()) {
            SPARQLModule.clearPlatformGraphs(sparqlService, graphsToClear);
        }

    }

    /**
     * @throws Exception if any Exception was encountered during context
     * shutdown.
     */
    public void shutdown() throws Exception {
        OpenSilex.getInstance().shutdown();
    }
}

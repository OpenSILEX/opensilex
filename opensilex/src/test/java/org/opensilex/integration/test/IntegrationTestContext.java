//******************************************************************************
//                          IntegrationTestContext.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.integration.test;

import java.util.ArrayList;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.mockito.Mockito;
import org.opensilex.OpenSilex;
import org.opensilex.server.rest.RestApplication;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

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

        resourceConfig = new RestApplication(OpenSilex.getInstance());

        // create a mock for HttpServletRequest which is not available with grizzly
        final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        resourceConfig.register(new AbstractBinder() {
            @Override
            protected void configure() {
                bind(request).to(HttpServletRequest.class);
            }
        });
    }

    public ResourceConfig getResourceConfig() {
        return resourceConfig;
    }

    List<Runnable> hooks = new ArrayList<>();

    public void addShutdownHook(Runnable hook) {
        hooks.add(hook);
    }

    /**
     * @throws Exception if any Exception was encountered during context
     * shutdown.
     */
    public void shutdown() throws Exception {
        hooks.forEach(hook -> hook.run());
        OpenSilex.getInstance().shutdown();
    }

}

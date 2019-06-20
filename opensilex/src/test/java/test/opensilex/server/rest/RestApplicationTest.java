//******************************************************************************
//                             RestApplicationTest.java
// OpenSILEX
// Copyright © INRA 2019
// Creation date: 01 jan. 2019
// Contact: morgane.vidal@inra.fr, vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package test.opensilex.server.rest;

import javax.servlet.http.HttpServletRequest;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.mockito.Mockito;
import org.opensilex.OpenSilex;
import org.opensilex.server.rest.RestApplication;


/**
 * The application test class. Initialize the required environment for the tests. 
 * Any other test class must extends OpensilexTest.
 */
public class RestApplicationTest extends JerseyTest {

    /**
     * Initialise the required environment for the tests
     * @return ResourceConfig instance
     */
    @Override
    protected ResourceConfig configure() {
        OpenSilex app = OpenSilex.getInstance(OpenSilex.getBaseDirectory(), OpenSilex.TEST_PROFILE_ID, null);
        ResourceConfig resourceConfig = new RestApplication(app);
        
        // create a mock for HttpServletRequest which is not available with grizzly
        final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        resourceConfig.register(new AbstractBinder() {
            @Override
            protected void configure() {
                bind(request).to(HttpServletRequest.class);
            }
        });
        
        return resourceConfig;
    }
}

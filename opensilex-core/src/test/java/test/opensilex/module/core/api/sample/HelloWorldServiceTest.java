//******************************************************************************
//                             RestApplicationTest.java
// OpenSILEX
// Copyright © INRA 2019
// Creation date: 01 jan. 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************

package test.opensilex.module.core.api.sample;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import test.opensilex.server.rest.RestApplicationTest;

/**
 * Test class for HelloWorldServiceTest
 */
public class HelloWorldServiceTest extends RestApplicationTest {

    @Test
    public void testHelloWorldResponse() {
        Response response = target("/hello").request().get();

        assertEquals("Http Response should be 200: ", Status.OK.getStatusCode(), response.getStatus());
        assertEquals("Http Content-Type should be: ", MediaType.TEXT_PLAIN, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        String content = response.readEntity(String.class);
        assertEquals("Content of response is: ", "Hello World !", content);
    }
}

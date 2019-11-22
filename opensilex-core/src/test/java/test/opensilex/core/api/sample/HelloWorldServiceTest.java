//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package test.opensilex.core.api.sample;

import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.Status;
import static org.junit.Assert.*;
import org.junit.*;
import test.opensilex.server.rest.*;


/**
 * Test class for HelloWorldServiceTest
 */
public class HelloWorldServiceTest extends RestApplicationTest {

    @Test
    public void testHelloWorldResponse() {
        Response response = target("/hello/world").request().get();
        System.out.println(response.getEntity());
        
        assertEquals("Http Response should be 200: ", Status.OK.getStatusCode(), response.getStatus());
        assertEquals("Http Content-Type should be: ", MediaType.TEXT_PLAIN, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        String content = response.readEntity(String.class);
        assertEquals("Content of response is: ", "Hello World !!", content);
    }
}

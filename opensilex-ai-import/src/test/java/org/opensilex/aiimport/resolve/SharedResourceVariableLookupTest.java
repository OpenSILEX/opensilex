//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.resolve;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensilex.core.CoreModule;
import org.opensilex.core.config.SharedResourceInstanceItem;
import org.opensilex.core.sharedResource.SharedResourceInstanceDTO;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The lookup on shared resource instances, against a stand-in instance answering the platform's own
 * authentication and variable search, and an instance nobody listens on.
 *
 * @author Arnaud Charleroy
 */
public class SharedResourceVariableLookupTest {

    private static final URI PHENOME = URI.create("http://phenome.test/rest");
    private static final URI OFFLINE = URI.create("http://offline.test/rest");

    private static HttpServer instance;

    @BeforeClass
    public static void aSharedInstance() throws IOException {
        instance = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        instance.createContext("/security/authenticate", exchange ->
                reply(exchange, "{\"metadata\":{},\"result\":{\"token\":\"stub-token\"}}"));
        // The client expands the instance's prefixed URIs, so it asks for its namespaces first.
        instance.createContext("/ontology/name_space", exchange ->
                reply(exchange, "{\"metadata\":{},\"result\":{\"phenome\":\"http://phenome.test/id/\"}}"));
        instance.createContext("/core/variables", exchange -> reply(exchange,
                "{\"metadata\":{\"pagination\":{\"pageSize\":5,\"currentPage\":0,\"totalCount\":2,\"totalPages\":1}},"
                        + "\"result\":["
                        + "{\"uri\":\"http://phenome.test/id/variable/plant_height\",\"name\":\"plant_height\","
                        + "\"alternative_name\":\"PH\"},"
                        + "{\"uri\":\"http://phenome.test/id/variable/plant_heights\",\"name\":\"plant_heights\"}]}"));
        instance.start();
    }

    @AfterClass
    public static void noMoreInstance() {
        instance.stop(0);
    }

    private static void reply(HttpExchange exchange, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    /**
     * A core module declaring two shared instances: one answering, one that does not exist.
     */
    private static CoreModule coreWithTwoInstances() {
        String reachable = "http://127.0.0.1:" + instance.getAddress().getPort();
        return new CoreModule() {
            @Override
            public List<SharedResourceInstanceDTO> getSharedResourceInstancesFromConfiguration(String lang) {
                return List.of(new SharedResourceInstanceDTO().setUri(PHENOME).setLabel("PHENOME"),
                        new SharedResourceInstanceDTO().setUri(OFFLINE));
            }

            @Override
            public SharedResourceInstanceItem getSharedResourceInstanceConfiguration(URI uri) {
                return item(uri, PHENOME.equals(uri) ? reachable : "http://127.0.0.1:1");
            }
        };
    }

    private static SharedResourceInstanceItem item(URI uri, String apiUrl) {
        return new SharedResourceInstanceItem() {
            @Override
            public String uri() {
                return uri.toString();
            }

            @Override
            public String apiUrl() {
                return apiUrl;
            }

            @Override
            public Map<String, String> label() {
                return Map.of("en", "instance");
            }

            @Override
            public String accountName() {
                return "guest@opensilex.org";
            }

            @Override
            public String accountPassword() {
                return "guest";
            }
        };
    }

    /**
     * The remote search is a regex: only an exact name or alternative name is kept, tagged with the
     * instance it came from; the instance nobody answers on is said once, then left alone.
     */
    @Test
    public void anExactMatchIsFoundAndAnUnreachableInstanceSaidOnce() {
        SharedResourceVariableLookup lookup = new SharedResourceVariableLookup(coreWithTwoInstances(), "en");
        assertTrue(lookup.isEnable());
        assertEquals(2, lookup.getInstances().size());

        List<String> warnings = new ArrayList<>();
        List<ResourceReference> found = lookup.search("PH", warnings);

        assertEquals(found.toString(), 1, found.size());
        assertEquals("plant_height", found.get(0).getName());
        assertEquals(PHENOME.toString(), found.get(0).getSharedResourceInstance());
        assertEquals("PHENOME", found.get(0).getSharedResourceInstanceLabel());
        assertEquals(1, warnings.size());
        assertTrue("an instance without a label is named by its URI", warnings.get(0).contains(OFFLINE.toString()));

        lookup.search("plant_height", warnings);
        assertEquals("the unreachable instance is not asked again", 1, warnings.size());
    }

    @Test
    public void anEmptyNameIsNotSearched() {
        SharedResourceVariableLookup lookup = new SharedResourceVariableLookup(coreWithTwoInstances(), "en");

        assertTrue(lookup.search("", new ArrayList<>()).isEmpty());
        assertTrue(lookup.search(null, new ArrayList<>()).isEmpty());
    }

    /**
     * A configuration that cannot be read leaves the lookup disabled rather than the analysis broken.
     */
    @Test
    public void anUnreadableConfigurationDisablesTheLookup() {
        SharedResourceVariableLookup lookup = new SharedResourceVariableLookup(new CoreModule() {
            @Override
            public List<SharedResourceInstanceDTO> getSharedResourceInstancesFromConfiguration(String lang) {
                throw new IllegalStateException("no configuration");
            }
        }, "en");

        assertFalse(lookup.isEnable());
        assertTrue(lookup.search("PH", new ArrayList<>()).isEmpty());
    }
}

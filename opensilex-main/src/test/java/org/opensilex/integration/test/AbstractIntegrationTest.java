//******************************************************************************
//                          AbstractIntegrationTest.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.integration.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.glassfish.jersey.server.ResourceConfig;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.PaginatedListResponse;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.client.Client;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.test.DeploymentContext;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.glassfish.jersey.test.grizzly.GrizzlyTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainer;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.mockito.Mockito;
import org.opensilex.OpenSilex;
import org.opensilex.server.rest.RestApplication;
import org.opensilex.server.rest.serialization.ObjectMapperContextResolver;
import org.opensilex.utils.OrderBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Renaud COLIN
 * @author Vincent MIGOT
 *
 * Abstract class used for API testing
 */
public abstract class AbstractIntegrationTest extends JerseyTest {

    protected final static Logger LOGGER = LoggerFactory.getLogger(AbstractIntegrationTest.class);

    @Rule
    public TestRule watcher = new TestWatcher() {
        protected void starting(Description description) {
            LOGGER.debug("\n\n####### Starting IT: " + description.getTestClass().getSimpleName() + " - " + description.getMethodName() + " #######");
        }
    };

    protected static OpenSilex opensilex;

    public static OpenSilex getOpensilex() {
        return opensilex;
    }

    protected final static ObjectMapper mapper = ObjectMapperContextResolver.getObjectMapper();

    @BeforeClass
    public static void createOpenSilex() throws Exception {
        Map<String, String> args = new HashMap<>();
        args.put(OpenSilex.PROFILE_ID_ARG_KEY, OpenSilex.TEST_PROFILE_ID);
        args.put(OpenSilex.NO_CACHE_ARG_KEY, "true");

        // NOTE: uncomment this line to enable full debug during integration tests       
        // args.put(OpenSilex.DEBUG_ARG_KEY, "true");
        LOGGER.debug("Create OpenSilex instance for Integration Test");
        opensilex = OpenSilex.createInstance(args);
    }

    
    // Variables defined to have a single test server start/stop per test class execution instead of per method
    // See: https://github.com/eclipse-ee4j/jersey/issues/4606
    
    private static TestContainerFactory testContainerFactory;
    private static TestContainer globalTestContainer = null;
    private static Client globalClient = null;
    
    @AfterClass
    public static void stopOpenSilex() throws Exception {
        try {
            if (globalTestContainer != null) {
                globalTestContainer.stop();
            }
        } finally {
            closeIfNotNull(globalClient);
        }
        globalTestContainer = null;
        globalClient = null;
        opensilex.shutdown();
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        if (globalClient == null) {
            globalClient = this.getClient();
        }

    }

    @Override
    protected Client getClient() {
        if (globalClient == null) {
            return super.getClient();
        } else {
            return globalClient;
        }
    }
    
    @Override
    protected TestContainerFactory getTestContainerFactory() throws TestContainerException {
        if (testContainerFactory == null) {
            testContainerFactory = new CustomTestContainerFactory();
        }
        return testContainerFactory;
    }

    private class CustomTestContainerFactory extends GrizzlyTestContainerFactory {

        @Override
        public TestContainer create(URI baseUri, DeploymentContext context) {
            if (globalTestContainer == null) {
                globalTestContainer = super.create(baseUri, context);
            }
            return globalTestContainer;
        }

    }

    @After
    public void tearDown() throws Exception {
        // DISABLE PARENT BEHAVIOR TO PREVENT SERVER STOP - IMPORTANT - DO NOT REMOVE
    }

    @Override
    public void configureClient(ClientConfig config) {
        config.register(ObjectMapperContextResolver.class);
    }

    @Override
    protected ResourceConfig configure() {

        enable(TestProperties.LOG_TRAFFIC);
        enable(TestProperties.DUMP_ENTITY);

        ResourceConfig resourceConfig;
        try {

            // initialize application
            resourceConfig = new RestApplication(opensilex);

            // create a mock for HttpServletRequest which is not available with grizzly
            final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
            resourceConfig.register(new AbstractBinder() {
                @Override
                protected void configure() {
                    bind(request).to(HttpServletRequest.class);
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return resourceConfig;
    }

    /**
     *
     * Get {@link Response} from a open POST service call.
     *
     * @param target the {@link WebTarget} on which POST the given entity
     * @param entity the data to POST on the given target
     * @return target invocation response.
     */
    protected Response getJsonPostPublicResponse(WebTarget target, Object entity) {
        return target.request(MediaType.APPLICATION_JSON).post(Entity.entity(entity, MediaType.APPLICATION_JSON_TYPE));
    }

    /**
     *
     * Get {@link Response} from a open PUT service call.
     *
     * @param target the {@link WebTarget} on which PUT the given entity
     * @param entity the data to PUT on the given target
     * @return target invocation response.
     */
    protected Response getJsonPutPublicResponse(WebTarget target, Object entity) {
        return target.request(MediaType.APPLICATION_JSON).put(Entity.entity(entity, MediaType.APPLICATION_JSON_TYPE));
    }

    /**
     *
     * Get {@link Response} from an open GET{uri} service call.
     *
     * @param target the {@link WebTarget} on which get an entity with the given URI
     * @param uri the URI of the resource to fetch from the given target.
     * @return target invocation response.
     *
     * @see WebTarget#resolveTemplate(String, Object)
     */
    protected Response getJsonGetByUriPublicResponse(WebTarget target, String uri) {
        return target.resolveTemplate("uri", uri).request(MediaType.APPLICATION_JSON).get();
    }

    /**
     *
     * Get {@link Response} from a public GET service call.
     *
     * @param target the {@link WebTarget} on which GET some content
     * @return target invocation response.
     */
    protected Response getJsonGetPublicResponse(WebTarget target) {
        return target.request(MediaType.APPLICATION_JSON).get();
    }

    /**
     *
     * Get {@link Response} from a public DELETE{uri} service call.
     *
     * @param target the {@link WebTarget} on which DELETE the given uri
     * @param uri the URI of the resource to DELETE
     *
     * @return target invocation response.
     *
     * @see WebTarget#resolveTemplate(String, Object)
     */
    protected Response getDeleteByUriPublicResponse(WebTarget target, String uri) {
        return target.resolveTemplate("uri", uri).request(MediaType.APPLICATION_JSON).delete();
    }

    /**
     * @see #appendSearchParams(WebTarget, Integer, Integer, List,Map)
     */
    protected WebTarget appendSearchParams(WebTarget target, Integer page, Integer pageSize, Map<String, Object> params) {
        return appendSearchParams(target, page, pageSize, Collections.emptyList(), params);
    }

    /**
     * Append pagination, ordering and a set of query params to a given {@link WebTarget}
     *
     * @param target the {@link WebTarget} on which append params
     * @param page the current page index
     * @param pageSize the page size
     * @param orderByList a list of {@link OrderBy} condition
     * @param params the map between param name and param value
     *
     * @return the updated {@link WebTarget}
     *
     * @see WebTarget#queryParam(String, Object...)
     * @see #appendQueryParams(WebTarget, Map)
     */
    protected WebTarget appendSearchParams(WebTarget target, Integer page, Integer pageSize, List<OrderBy> orderByList, Map<String, Object> params) {

        target.queryParam("page", page)
                .queryParam("pageSize", pageSize)
                .queryParam("orderBy", orderByList);

        return appendQueryParams(target, params);
    }

    /**
     * Append a set of query params to a given {@link WebTarget}
     *
     * @param target the {@link WebTarget} on which append params
     * @param params the map between param name and param value
     *
     * @return the updated {@link WebTarget}
     */
    protected WebTarget appendQueryParams(WebTarget target, Map<String, Object> params) {

        for (Map.Entry<String, Object> entry : params.entrySet()) {
            Object value = entry.getValue();
            if (List.class
                    .isAssignableFrom(value.getClass())) {
                for (Object v : ((List<?>) value)) {
                    target = target.queryParam(entry.getKey(), v);
                }
            } else {
                target = target.queryParam(entry.getKey(), entry.getValue());
            }
        }
        return target;
    }

    /**
     * This method try to extract an URI from the given {@link Response} and is expecting that the Response describe a {@link ObjectUriResponse}
     *
     * @param response the Response on which we want to extract URI
     * @return the URI extracted from the given Response
     *
     * @throws URISyntaxException if the extracted URI as String could not be parse as an {@link URI}
     */
    protected URI extractUriFromResponse(final Response response) throws URISyntaxException {
        JsonNode node = response.readEntity(JsonNode.class);
        ObjectUriResponse postResponse = mapper.convertValue(node, ObjectUriResponse.class);
        String uri = postResponse.getResult();
        uri = uri.replace("[", "");
        uri = uri.replace("]", "");
        return new URI(uri);
    }
    
    /**
     * This method try to extract an URI from the given {@link Response} and is expecting that the Response describe a {@link PaginatedListResponse}
     *
     * @param response the Response on which we want to extract an URI List
     * @return the List of URI extracted from the given Response
     *
     */
    protected List<URI> extractUriListFromPaginatedListResponse(final Response response) {
        JsonNode node = response.readEntity(JsonNode.class);
        PaginatedListResponse<URI> listResponse = mapper.convertValue(node, new TypeReference<PaginatedListResponse<URI>>() {
        });
        return listResponse.getResult();
    }

    protected boolean compareMaps(Map<String, String> first, Map<String, String> second) {
        if (first.size() != second.size()) {
            return false;
        }

        return first.entrySet().stream()
                .allMatch(e -> e.getValue().equals(second.get(e.getKey())));
    }

    protected void printJsonNode(JsonNode node) throws JsonProcessingException {
        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
        System.out.println(json);
    }
}

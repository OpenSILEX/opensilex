//******************************************************************************
//                          AbstractIntegrationTest.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.integration.test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.DeploymentContext;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.glassfish.jersey.test.grizzly.GrizzlyTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainer;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.junit.*;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.mockito.Mockito;
import org.opensilex.OpenSilex;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.rest.RestApplication;
import org.opensilex.server.rest.serialization.ObjectMapperContextResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.assertTrue;

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
//         args.put(OpenSilex.DEBUG_ARG_KEY, "true");
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

    private static class CustomTestContainerFactory extends GrizzlyTestContainerFactory {

        @Override
        public TestContainer create(URI baseUri, DeploymentContext context) {
            if (globalTestContainer == null) {
                globalTestContainer = super.create(baseUri, context);
            }
            return globalTestContainer;
        }

    }

    @After
    public void tearDown() {
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
     * This method try to extract a URI from the given {@link Response} and is expecting that the Response describe a {@link ObjectUriResponse}
     *
     * @param response the Response on which we want to extract URI
     * @return the URI extracted from the given Response
     *
     * @throws URISyntaxException if the extracted URI as String could not be parsed as an {@link URI}
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
     * This method try to extract a URI from the given {@link Response} and is expecting that the Response describe a {@link PaginatedListResponse}
     *
     * @param response the Response on which we want to extract a URI List
     * @return the List of URI extracted from the given Response
     *
     */
    protected List<URI> extractUriListFromPaginatedListResponse(final Response response) {
        JsonNode node = response.readEntity(JsonNode.class);
        PaginatedListResponse<URI> listResponse = mapper.convertValue(node, new TypeReference<>() {
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

    // TODO : restart from here
    public class PublicCall {
        private Map<String, Object> params = new HashMap<>();
        private Object body = null;
        private Map<String, Object> pathTemplateParams = new HashMap<>(); // Most of the time this is used for GET or DELETE by URI : {"uri":"myResourceUri"}
        private Method serviceMethod;
        private String pathTemplate;

        private String httpMethod;

        public PublicCall(Method serviceMethod, String pathTemplate, String httpMethod) {
            this.serviceMethod = serviceMethod;
            this.pathTemplate = pathTemplate;
            this.httpMethod = httpMethod;
        }

        public PublicCall setParams(Map<String, Object> params) {
            this.params = params;
            return this;
        }

        public PublicCall setParam(String key, Object value) {
            this.params.put(key, value);
            return this;
        }

        public PublicCall setBody(Object body) {
            this.body = body;
            return this;
        }

        public PublicCall setPathTemplateParam(String key, Object value) {
            this.pathTemplateParams.put(key, value);
            return this;
        }

        public Response executeCall(){
            httpMethod = findHttpMethod(serviceMethod);
            WebTarget target = createTarget(params, pathTemplateParams, serviceMethod, pathTemplate);
            return makeCorrectCall(target, httpMethod, body);
        }
        protected <T> Result <T> executeCallAndDeserialize(TypeReference<T> typeReference) {

            Response response = executeCall();

            assertTrue(response.getStatus() >= 200 && response.getStatus() < 300);

            return new Result<>(readResponse(response, typeReference, serviceMethod), response);
        }

        public class Result <T> {
            private T deserializedResponse;
            private Response response;

            private Result(T deserializedResponse, Response response) {
                this.deserializedResponse = deserializedResponse;
                this.response = response;
            }

            public T getDeserializedResponse() {
                return deserializedResponse;
            }

            public Response getResponse() {
                return response;
            }
        }
    }

    protected String findHttpMethod(Method serviceMethod) {
        if (serviceMethod.isAnnotationPresent(GET.class)) {
            return HttpMethod.GET;
        } else if (serviceMethod.isAnnotationPresent(POST.class)) {
            return HttpMethod.POST;
        } else if (serviceMethod.isAnnotationPresent(PUT.class)) {
            return HttpMethod.PUT;
        } else if (serviceMethod.isAnnotationPresent(DELETE.class)) {
            return HttpMethod.DELETE;
        } else {
            throw new UnsupportedOperationException("HTTP method not supported");
        }
    }

    protected Response makeCorrectCall(WebTarget target, String httpMethod, Object body) {
        Invocation.Builder requestBuilder = target.request(MediaType.APPLICATION_JSON);
        if(Objects.equals(httpMethod, HttpMethod.GET)) {
            return requestBuilder.get();
        } else if(Objects.equals(httpMethod, HttpMethod.POST)) {
            return requestBuilder.post(Entity.entity(body, MediaType.APPLICATION_JSON_TYPE));
        } else if(Objects.equals(httpMethod, HttpMethod.PUT)) {
            return requestBuilder.put(Entity.entity(body, MediaType.APPLICATION_JSON_TYPE));
        } else if(Objects.equals(httpMethod, HttpMethod.DELETE)) {
            return requestBuilder.delete();
        } else {
            throw new UnsupportedOperationException("HTTP method not supported");
        }
    }

    protected WebTarget createTarget(Map<String, Object> params, Map<String, Object> pathTemplateParams, Method serviceMethod, String searchPath) {
        WebTarget target = target(searchPath);
        if (!params.isEmpty()){
            checkParamsExist(params, serviceMethod);
            appendQueryParams(target, params);
        }
        if (!pathTemplateParams.isEmpty()) {
            target = target.resolveTemplates(pathTemplateParams);
        }
        return target;
    }

    protected void checkParamsExist(Map<String, Object> params, Method serviceMethod) {
        List<String> availableParams = Arrays.stream(serviceMethod.getParameters())
                .map(parameter -> parameter.getAnnotation(QueryParam.class).value())
                .collect(Collectors.toList());
        assertTrue(availableParams.containsAll(params.keySet()));
    }


    protected <T> T readResponse(Response response, TypeReference<T> type, Method method) {
        JsonNode node = response.readEntity(JsonNode.class);

        // Verify compatibility between TypeReference and method's return type
        Type expectedType = method.getGenericReturnType();
        TypeReference<?> expectedTypeReference = new TypeReference<>() {
            @Override
            public Type getType() {
                return expectedType;
            }
        };
        Assert.assertEquals(type.getType(), expectedTypeReference.getType());

        return mapper.convertValue(node, type);
    }
}

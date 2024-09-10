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
import org.opensilex.server.response.JsonResponse;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.rest.RestApplication;
import org.opensilex.server.rest.serialization.ObjectMapperContextResolver;
import org.opensilex.utils.OrderBy;
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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
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
                //@todo find a better way to configure this
                java.util.logging.Logger.getLogger("org.glassfish").setLevel(Level.SEVERE);
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
     * @see #appendSearchParams(WebTarget, Integer, Integer, List,Map)
     */
    @Deprecated
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
    @Deprecated
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
    @Deprecated
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
    @Deprecated
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
    @Deprecated
    protected List<URI> extractUriListFromPaginatedListResponse(final Response response) {
        JsonNode node = response.readEntity(JsonNode.class);
        PaginatedListResponse<URI> listResponse = mapper.convertValue(node, new TypeReference<>() {
        });
        return listResponse.getResult();
    }

    @Deprecated
    protected <T> T readResponse(Response response, TypeReference<T> type) {
        JsonNode node = response.readEntity(JsonNode.class);
        return mapper.convertValue(node, type);
    }

    /**
     * This class represents a public call and can be used to perform HTTP requests.
     */
    public class PublicCall {
        protected final Map<String, Object> params;
        protected final Object body;
        protected final Map<String, Object> pathTemplateParams; // Most of the time this is used for GET or DELETE by URI : {"uri":"myResourceUri"}
        protected final Method serviceMethod;
        protected final String pathTemplate;
        protected final String httpMethod;
        protected final MediaType callMediaType;
        protected final List<MediaType> responseMediaTypes;

        public PublicCall(
                Map<String, Object> params,
                Object body,
                Map<String, Object> pathTemplateParams,
                Method serviceMethod,
                String pathTemplate,
                String httpMethod,
                MediaType callMediaType,
                List<MediaType> responseMediaTypes
        ) {
            this.params = params;
            this.body = body;
            this.pathTemplateParams = pathTemplateParams;
            this.serviceMethod = serviceMethod;
            this.pathTemplate = pathTemplate;
            this.httpMethod = httpMethod;
            this.callMediaType = callMediaType;
            this.responseMediaTypes = responseMediaTypes;
        }

        /**
         * Executes the call and returns the raw response.
         * @return the response of the call.
         * @throws Exception if there is an error executing the call
         */
        public Response executeCall() throws Exception {
            WebTarget target = createTarget();
            return makeCorrectCall(target);
        }

        /**
         * Executes the call and asserts that the response status matches the expected status.
         * @param expectedStatus the expected status of the response
         * @throws RuntimeException if there is an error executing the call or if the response status does not match the expected status
         */
        public void executeCallAndAssertStatus(Response.Status expectedStatus) {
            try (Response response = executeCall()) {
                assertEquals(expectedStatus.getStatusCode(), response.getStatus());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * Executes the call and deserializes the response into a specified type.
         * @param typeReference the type to deserialize the response into
         * @return the deserialized response
         * @throws Exception if there is an error executing the call or deserializing the response
         */
        public <T extends JsonResponse<?>> Result<T> executeCallAndDeserialize(TypeReference<T> typeReference) throws Exception {
            try (Response response = executeCall()) {
                assertTrue("request failed with status : "+response.getStatus(), response.getStatus() >= 200 && response.getStatus() < 300);
                Result<T> result = new Result<>(readResponse(response, typeReference), response);
                response.close();
                return result;
            }
        }

        /**
         * Executes the call and returns the URI from the response.
         * @return the URI from the response
         * @throws Exception if there is an error executing the call or retrieving the URI from the response
         */
        public URI executeCallAndReturnURI() throws Exception {
            if (Objects.equals(httpMethod, HttpMethod.PUT) ||
                    Objects.equals(httpMethod, HttpMethod.POST) ||
                    Objects.equals(httpMethod, HttpMethod.DELETE)) {
                Result<ObjectUriResponse> response = executeCallAndDeserialize(new TypeReference<>() {
                });
                return URI.create(response.getDeserializedResponse().getResult());
            } else {
                Result<DeserializedResponse<UriResourceDTO>> readResponse = executeCallAndDeserialize(new TypeReference<>() {
                });
                return readResponse.getDeserializedResponse().getResult().getUri();
            }
        }

        /**
         * Makes the correct call based on the target.
         * @param target the target for the call
         * @return the response of the call
         */
        protected Response makeCorrectCall(WebTarget target) {
            Invocation.Builder requestBuilder = buildRequestBuilder(target);
            return executeRequest(requestBuilder);
        }

        /**
         * Builds a request builder based on the target.
         * @param target the target for the call
         * @return the request builder
         */
        protected Invocation.Builder buildRequestBuilder(WebTarget target) {

            Invocation.Builder requestBuilder = target.request(callMediaType);

            requestBuilder.accept(responseMediaTypes.toArray(new MediaType[0]));

            return requestBuilder;
        }

        /**
         * Executes the request based on the request builder.
         * @param requestBuilder the request builder for the call
         * @return the response of the call
         * @throws UnsupportedOperationException if the HTTP method is not supported
         */
        protected Response executeRequest(Invocation.Builder requestBuilder) {
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

        /**
         * Creates a target for the call.
         * @return the target for the call
         */
        protected WebTarget createTarget() {
            WebTarget target = target(pathTemplate);
            if (!params.isEmpty()){
                target = appendQueryParams(target);
            }
            if (!pathTemplateParams.isEmpty()) {
                target = target.resolveTemplates(pathTemplateParams);
            }
            return target;
        }

        /**
         * Appends query parameters to the target.
         * @param target the target for the call
         * @return the target with appended query parameters
         */
        protected WebTarget appendQueryParams(WebTarget target) {

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
         * Reads the response and converts it into a specified type.
         * @param response the response to read
         * @param type the type to convert the response into
         * @return the converted response
         */
        protected <T> T readResponse(Response response, TypeReference<T> type) {
            JsonNode node = response.readEntity(JsonNode.class);

            return mapper.convertValue(node, type);
        }

        public JsonNode executeCallAnReturnJsonNode() throws Exception {
            try (Response response = executeCall()) {
                JsonNode node = response.readEntity(JsonNode.class);
                response.close();
                return node;
            }
        }


        @Override
        public String toString() {
            return "PublicCall{" +
                    "params=" + params +
                    ", body=" + body +
                    ", pathTemplateParams=" + pathTemplateParams +
                    ", serviceMethod=" + serviceMethod +
                    ", pathTemplate='" + pathTemplate + '\'' +
                    ", httpMethod='" + httpMethod + '\'' +
                    ", callMediaType=" + callMediaType +
                    ", responseMediaTypes=" + responseMediaTypes +
                    '}';
        }
    }

    protected class PublicCallBuilder<T extends PublicCallBuilder<T>> {

        protected Map<String, Object> params = new HashMap<>();
        protected Object body = null;
        protected Map<String, Object> pathTemplateParams = new HashMap<>();
        protected Method serviceMethod;
        protected String pathTemplate;

        protected MediaType callMediaType = MediaType.valueOf(MediaType.APPLICATION_JSON);
        protected List<MediaType> responseMediaTypes = Collections.singletonList(MediaType.valueOf(MediaType.APPLICATION_JSON));
        protected String httpMethod;

        public PublicCallBuilder(ServiceDescription serviceDescription) {
            this.serviceMethod = serviceDescription.getServiceMethod();
            this.pathTemplate = serviceDescription.getPathTemplate();
        }

        public T setParams(Map<String, Object> params) {
            this.params = params;
            return self();
        }

        public T addParam(String key, Object value) {
            this.params.put(key, value);
            return self();
        }

        public T setBody(Object body) {
            this.body = body;
            return self();
        }

        public T setPathTemplateParams(Map<String, Object> pathTemplateParams) {
            this.pathTemplateParams = pathTemplateParams;
            return self();
        }

        public T addPathTemplateParam(String key, Object value) {
            this.pathTemplateParams.put(key, value);
            return self();
        }

        public T setUriInPath(URI uri) {
            this.pathTemplateParams.put("uri", uri);
            return self();
        }

        public T setCallMediaType(MediaType callMediaType) {
            this.callMediaType = callMediaType;
            return self();
        }

        public T setResponseMediaTypes(List<MediaType> responseMediaTypes) {
            this.responseMediaTypes = responseMediaTypes;
            return self();
        }

        public T addResponseMediaType(MediaType responseMediaType) {
            this.responseMediaTypes.add(responseMediaType);
            return self();
        }

        @SuppressWarnings("unchecked")
        protected T self() {
            return (T) this;
        }

        public PublicCall build() {
            preBuildChecks();
            return new PublicCall(params, body, pathTemplateParams, serviceMethod, pathTemplate, httpMethod, callMediaType, responseMediaTypes);
        }

        /**
         * A method to perform pre-build checks : check if the parameters exist for the service method, find the HTTP method, check if the path parameters exist
         */
        protected void preBuildChecks() {
            checkParamsExist();
            findHttpMethod();
            checkPathParamsExist();
        }

        /**
         * Checks if the parameters exist for the service method.
         * @throws AssertionError if not all parameters exist for the service method
         */
        protected void checkParamsExist() {
            List<String> availableParams = Arrays.stream(serviceMethod.getParameters())
                    .filter(parameter -> parameter.getAnnotation(QueryParam.class) != null)
                    .map(parameter -> parameter.getAnnotation(QueryParam.class).value())
                    .collect(Collectors.toList());
            assertTrue(availableParams.containsAll(params.keySet()));
        }

        /**
         * Finds the HTTP method for the service method.
         * @throws UnsupportedOperationException if the HTTP method is not supported
         */
        protected void findHttpMethod() {
            if (serviceMethod.isAnnotationPresent(GET.class)) {
                httpMethod = HttpMethod.GET;
            } else if (serviceMethod.isAnnotationPresent(POST.class)) {
                httpMethod = HttpMethod.POST;
            } else if (serviceMethod.isAnnotationPresent(PUT.class)) {
                httpMethod = HttpMethod.PUT;
            } else if (serviceMethod.isAnnotationPresent(DELETE.class)) {
                httpMethod = HttpMethod.DELETE;
            } else {
                throw new UnsupportedOperationException("HTTP method not supported");
            }
        }

        /**
         * Method to check if all path parameters exist in the path template.
         */
        protected void checkPathParamsExist() {
            assertTrue("one or many path parameter does was given but does not exists for this endpoint", pathTemplateParams.entrySet().stream().allMatch(entry -> pathTemplate.contains("{" + entry.getKey() + "}")));
        }
    }

    /**
     * This class represents the result of a call in two parts : the raw response and the deserialized one.
     */
    public static class Result<T> {
        private final T deserializedResponse;
        private final Response response;

        public Result(T deserializedResponse, Response response) {
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

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
import java.util.*;
import java.util.stream.Collectors;

import static junit.framework.TestCase.assertEquals;
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

    protected boolean compareMaps(Map<String, String> first, Map<String, String> second) {
        if (first.size() != second.size()) {
            return false;
        }

        return first.entrySet().stream()
                .allMatch(e -> e.getValue().equals(second.get(e.getKey())));
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

        protected String httpMethod;

        protected final MediaType callMediaType;
        protected final List<MediaType> responseMediaTypes;

        public PublicCall(Map<String, Object> params, Object body, Map<String, Object> pathTemplateParams, Method serviceMethod, String pathTemplate, MediaType callMediaType, List<MediaType> responseMediaTypes) {
            this.params = params;
            this.body = body;
            this.pathTemplateParams = pathTemplateParams;
            this.serviceMethod = serviceMethod;
            this.pathTemplate = pathTemplate;
            this.callMediaType = callMediaType;
            this.responseMediaTypes = responseMediaTypes;
        }

        /**
         * Executes the call and returns the raw response.
         *
         * @return the response of the call.
         */
        public Response executeCall() throws Exception {
            httpMethod = findHttpMethod(serviceMethod);
            WebTarget target = createTarget(params, pathTemplateParams, serviceMethod, pathTemplate);
            return makeCorrectCall(target, httpMethod, body, callMediaType, responseMediaTypes);
        }

        /**
         * Executes the call, deserializes the response and returns the result.
         *
         * @param typeReference the type reference for deserialization.
         * @return the result of the call.
         */
        public <T> Result <T> executeCallAndDeserialize(TypeReference<T> typeReference) throws Exception {

            Response response = executeCall();

            assertTrue(response.getStatus() >= 200 && response.getStatus() < 300);

            return new Result<>(readResponse(response, typeReference, serviceMethod), response);
        }

        /**
         * Finds the HTTP method for the specified service method.
         *
         * @param serviceMethod the service method to find the HTTP method for.
         * @return the HTTP method of the service method.
         */
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

        /**
         * Makes the correct call based on the specified target, HTTP method and body.
         *
         * @param target the target of the call.
         * @param httpMethod the HTTP method of the call.
         * @param body the body of the call.
         * @return the response of the call.
         */
        protected Response makeCorrectCall(WebTarget target, String httpMethod, Object body, MediaType callMediaType, List<MediaType> responseMediaTypes) {

            Invocation.Builder requestBuilder;
            if (Objects.isNull(callMediaType)) {
                requestBuilder = target.request(MediaType.APPLICATION_JSON);
            } else {
                requestBuilder = target.request(callMediaType);
            }

            if (!responseMediaTypes.isEmpty()) {
                for (MediaType mediaType : responseMediaTypes) {
                    requestBuilder.accept(mediaType);
                }
            }

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
         * Creates the target for the call based on the specified parameters, path template parameters, service method and
         * path template.
         *
         * @param params the parameters of the call.
         * @param pathTemplateParams the path template parameters of the call.
         * @param serviceMethod the service method of the call.
         * @param pathTemplate the path template of the call.
         * @return the target of the call.
         */
        protected WebTarget createTarget(Map<String, Object> params, Map<String, Object> pathTemplateParams, Method serviceMethod, String pathTemplate) {
            WebTarget target = target(pathTemplate);
            if (!params.isEmpty()){
                checkParamsExist(params, serviceMethod);
                appendQueryParams(target, params);
            }
            if (!pathTemplateParams.isEmpty()) {
                target = target.resolveTemplates(pathTemplateParams);
            }
            return target;
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
         * Checks if the specified parameters exist in the service method.
         *
         * @param params the parameters to check.
         * @param serviceMethod the service method to check in.
         */
        protected void checkParamsExist(Map<String, Object> params, Method serviceMethod) {
            List<String> availableParams = Arrays.stream(serviceMethod.getParameters())
                    .map(parameter -> parameter.getAnnotation(QueryParam.class).value())
                    .collect(Collectors.toList());
            assertTrue(availableParams.containsAll(params.keySet()));
        }

        /**
         * Reads the response, verifies the compatibility between the TypeReference and method's return type, and returns the deserialized response.
         *
         * @param response the response to read.
         * @param type the type reference for deserialization.
         * @param method the method to check compatibility with.
         * @return the deserialized response.
         */
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

    protected class PublicCallBuilder<T extends PublicCallBuilder<T>> {

        protected Map<String, Object> params = new HashMap<>();
        protected Object body = null;
        protected Map<String, Object> pathTemplateParams = new HashMap<>(); // Most of the time this is used for GET or DELETE by URI : {"uri":"myResourceUri"}
        protected Method serviceMethod;
        protected String pathTemplate;

        protected MediaType callMediaType = null;
        protected List<MediaType> responseMediaTypes = null;

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

        public T setUriInPath(String uri) {
            this.pathTemplateParams.put("{uri}", uri);
            return self();
        }

        public T setPathTemplate(String pathTemplate) {
            this.pathTemplate = pathTemplate;
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
            return new PublicCall(params, body, pathTemplateParams, serviceMethod, pathTemplate, callMediaType, responseMediaTypes);
        }
    }

    /**
     * This class represents the result of a call in two parts : the raw response and the deserialized one.
     */
    public class Result <T> {
        private T deserializedResponse;
        private Response response;

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

    public static class ServiceDescription {

        private final Method serviceMethod;
        private final String pathTemplate;

        public ServiceDescription(Method serviceMethod, String pathTemplate) {
            this.serviceMethod = serviceMethod;
            this.pathTemplate = pathTemplate;
        }

        public Method getServiceMethod() {
            return serviceMethod;
        }

        public String getPathTemplate() {
            return pathTemplate;
        }
    }
}

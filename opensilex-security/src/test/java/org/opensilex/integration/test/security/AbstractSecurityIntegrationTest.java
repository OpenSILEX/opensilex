//******************************************************************************
//                          AbstractIntegrationTest.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.integration.test.security;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.After;
import org.junit.BeforeClass;
import org.opensilex.integration.test.AbstractIntegrationTest;
import org.opensilex.security.SecurityModule;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.AuthenticationService;
import org.opensilex.security.authentication.api.AuthenticationAPI;
import org.opensilex.security.authentication.api.AuthenticationDTO;
import org.opensilex.security.authentication.api.TokenGetDTO;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.response.NamedResourceDTO;
import org.opensilex.sparql.response.ResourceDTO;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.service.SPARQLServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.*;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.opensilex.security.SecurityModule.DEFAULT_SUPER_ADMIN_EMAIL;
import static org.opensilex.security.SecurityModule.DEFAULT_SUPER_ADMIN_PASSWORD;

/**
 * @author Vincent MIGOT
 * <p>
 * Abstract class used for Secure API testing
 */
public abstract class AbstractSecurityIntegrationTest extends AbstractIntegrationTest {

    protected final static Logger LOGGER = LoggerFactory.getLogger(AbstractSecurityIntegrationTest.class);

    protected static SecurityModule securityModule;

    protected static final ServiceDescription authenticate;

    static {
        try {
            authenticate = new ServiceDescription(
                    AuthenticationAPI.class.getMethod("authenticate", AuthenticationDTO.class),
                    AuthenticationAPI.PATH + "/" + AuthenticationAPI.AUTHENTICATE_PATH
            );
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeClass
    public static void createAdmin() throws Exception {
        securityModule = opensilex.getModuleByClass(SecurityModule.class);
        securityModule.createDefaultSuperAdmin();
    }

    @After
    public void clearGraphs() throws Exception {
        clearGraphs(getModelsToClean());
        this.afterEach();
    }

    public void afterEach() throws Exception {

    }

    private SPARQLService sparql;

    public SPARQLService getSparqlService() {
        if (sparql == null) {
            sparql = getOpensilex().getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class).provide();
        }

        return sparql;
    }

    public static SPARQLService newSparqlService(){
        return getOpensilex().getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class).provide();
    }

    private AuthenticationService authentication;

    public AuthenticationService getAuthenticationService() {
        if (authentication == null) {
            authentication = getOpensilex().getServiceInstance(AuthenticationService.DEFAULT_AUTHENTICATION_SERVICE, AuthenticationService.class);
        }

        return authentication;
    }

    /**
     * Clear the list of SPARQL graph to clear after each test execution
     *
     * @param modelsToClear List of graphs to clear
     * @throws Exception in case of error happening during graph clear
     */
    public void clearGraphs(List<Class<? extends SPARQLResourceModel>> modelsToClear) throws Exception {
        SPARQLService sparqlService = getSparqlService();
        List<String> graphsToClean = new ArrayList<>(modelsToClear.size());
        for (Class<? extends SPARQLResourceModel> modelClass : modelsToClear) {
            URI graphURI = sparqlService.getDefaultGraphURI(modelClass);
            graphsToClean.add(graphURI.toString());
        }

        sparqlService.clearGraphs(graphsToClean.toArray(new String[graphsToClean.size()]));
    }

    /**
     * Default Abstract method to be implemented by subclasses for graph clear after tests
     *
     * @return the List of SPARQL Model to clear after each test execution.
     */
    protected List<Class<? extends SPARQLResourceModel>> getModelsToClean() {
        return new ArrayList<>();
    }
    private final HashMap<String, TokenGetDTO> tokenMap = new HashMap<>();

    protected <T extends SPARQLNamedResourceModel<T>> void testBasicCRUDAsAdmin(
            ServiceDescription createServiceDescription,
            ServiceDescription readServiceDescription,
            ServiceDescription updateServiceDescription,
            ServiceDescription deleteServiceDescription,
            NamedResourceDTO<T> entityToPost, NamedResourceDTO<T> entityToPut,
            NamedResourceDTO<T> entityExpectedAfterPut
    ) throws Exception {
        // CREATE
        UserCall createCall = new UserCallBuilder(createServiceDescription)
                .setBody(entityToPost)
                .buildAdmin();
        URI createdUri = createCall.executeCallAndReturnURI();

        // READ
        UserCall readCall = new UserCallBuilder(readServiceDescription)
                .setUriInPath(createdUri.toString())
                .buildAdmin();
        readCall.executeCallAndReturnURI();

        // UPDATE
        entityToPut.setUri(createdUri);
        UserCall updateCall = new UserCallBuilder(updateServiceDescription)
                .setBody(entityToPut)
                .buildAdmin();
        updateCall.executeCallAndReturnURI();
        NamedResourceDTO<T> readResponse = readCall.executeCallAndDeserialize(new TypeReference<NamedResourceDTO<T>>() {
        }).getDeserializedResponse();
        assertEquals(readResponse, entityExpectedAfterPut);

        // DELETE
        UserCall deleteCall = new UserCallBuilder(deleteServiceDescription)
                .setUriInPath(createdUri.toString())
                .buildAdmin();
        deleteCall.executeCallAndDeserialize(new TypeReference<ResourceDTO<?>>() {
        });
        Response result = readCall.executeCall();
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), result.getStatus());
    }

    protected <T extends SPARQLNamedResourceModel<T>> void testBasicCRUDListAsAdmin(
            ServiceDescription createServiceDescription,
            ServiceDescription readServiceDescription,
            ServiceDescription updateServiceDescription,
            ServiceDescription deleteServiceDescription,
            List<NamedResourceDTO<T>> entitiesToPost, List<NamedResourceDTO<T>> entitiesToPut,
            List<NamedResourceDTO<T>> entitiesExpectedAfterPut
    ) throws Exception {
        if (entitiesToPost.size() != entitiesToPut.size()) {
            for (int i = 0; i < entitiesToPost.size(); i++) {
                testBasicCRUDAsAdmin(
                        createServiceDescription,
                        readServiceDescription,
                        updateServiceDescription,
                        deleteServiceDescription,
                        entitiesToPost.get(i), entitiesToPut.get(i),
                        entitiesExpectedAfterPut.get(i)
                );
            }
        }
    }

    public class UserCall extends PublicCall {

        private final String userEmail;
        private final String userPassword;

        /**
         * Constructs a new UserCall with the specified service method, path template and HTTP method.
         *
         * @param serviceMethod the method of the webservice (e.g. ExperimentAPI.searchExperiments for the experiment
         *                      webservice. -> use ExperimentAPI.class.getMethod())
         * @param pathTemplate  the path template for the webservice. Can be a regular path or a template that contains
         *                      parts to replace (e.g. /core/experiments/{uri}. {uri} is a placeholder to be replaced by
         *                      the actual resource's URI)
         */
        public UserCall(Map<String, Object> params, Object body, Map<String, Object> pathTemplateParams,
                        Method serviceMethod, String pathTemplate, String userEmail, MediaType callMediaType,
                        List<MediaType> responseMediaTypes, String userPassword) {
            super(params, body, pathTemplateParams, serviceMethod, pathTemplate, callMediaType, responseMediaTypes);
            this.userEmail = userEmail;
            this.userPassword = userPassword;
        }

        @Override
        public Response executeCall() throws Exception {
            httpMethod = findHttpMethod(serviceMethod);
            WebTarget target = createTarget(params, pathTemplateParams, serviceMethod, pathTemplate);
            authenticateAndRegisterIfNecessary(userEmail, userPassword);
            appendToken(target, userEmail);
            return makeCorrectCall(target, httpMethod, body, callMediaType, responseMediaTypes);
        }

        @Override
        public <T> Result <T> executeCallAndDeserialize(TypeReference<T> typeReference) throws Exception {
            Response response = executeCall();
            assertTrue(response.getStatus() >= 200 && response.getStatus() < 300);
            return new Result<>(readResponse(response, typeReference, serviceMethod), response);
        }

        public URI executeCallAndReturnURI() throws Exception {
            if (Objects.equals(httpMethod, HttpMethod.PUT) || Objects.equals(httpMethod, HttpMethod.POST)) {
                Result<ObjectUriResponse> response = executeCallAndDeserialize(new TypeReference<ObjectUriResponse>() {
                });
                return URI.create(response.getDeserializedResponse().getResult());
            } else {
                Result<ResourceDTO<?>> readResponse = executeCallAndDeserialize(new TypeReference<ResourceDTO<?>>() {
                });
                return readResponse.getDeserializedResponse().getUri();
            }
        }

        protected Invocation.Builder appendToken(WebTarget target, String userEmail) throws IllegalArgumentException {
            if (!tokenMap.containsKey(userEmail)) {
                throw new IllegalArgumentException("Cannot find a token for user " + userEmail + ". Please generate a token using `registerToken` before your test.");
            }

            return target.request(MediaType.APPLICATION_JSON_TYPE)
                    .header(ApiProtected.HEADER_NAME, ApiProtected.TOKEN_PARAMETER_PREFIX + tokenMap.get(userEmail).getToken());
        }

        protected void authenticateAndRegisterIfNecessary(String userEmail, String userPassword) throws Exception {

            if (!tokenMap.containsKey(userEmail)){
                AuthenticationDTO authDto = new AuthenticationDTO();
                authDto.setIdentifier(userEmail);
                authDto.setPassword(userPassword);

                PublicCall tokenCall = new UserCallBuilder(authenticate).setBody(authDto).build();

                Result<SingleObjectResponse<TokenGetDTO>> postResult = tokenCall.executeCallAndDeserialize(
                        new TypeReference<SingleObjectResponse<TokenGetDTO>>() {
                        }
                );
                TokenGetDTO userToken = postResult.getDeserializedResponse().getResult();
                tokenMap.put(userEmail, userToken);
            }
        }
    }

    public class UserCallBuilder extends PublicCallBuilder<UserCallBuilder> {

        private String userEmail;
        private String userPassword;

        public UserCallBuilder setUserEmail(String userEmail) {
            this.userEmail = userEmail;
            return self();
        }

        public UserCallBuilder setUserPassword(String userPassword) {
            this.userPassword = userPassword;
            return self();
        }

        public UserCallBuilder(ServiceDescription serviceDescription) {
            super(serviceDescription);
        }

        @Override
        protected UserCallBuilder self() {
            return this;
        }

        @Override
        public UserCall build() {
            return new UserCall(
                    params,
                    body,
                    pathTemplateParams,
                    serviceMethod,
                    pathTemplate,
                    userEmail,
                    callMediaType,
                    responseMediaTypes,
                    userPassword
            );
        }

        public UserCall buildAdmin() {
            return new UserCall(
                    params,
                    body,
                    pathTemplateParams,
                    serviceMethod,
                    pathTemplate,
                    DEFAULT_SUPER_ADMIN_EMAIL,
                    callMediaType,
                    responseMediaTypes,
                    DEFAULT_SUPER_ADMIN_PASSWORD
            );
        }
    }
}

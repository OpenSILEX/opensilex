//******************************************************************************
//                          AbstractIntegrationTest.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.integration.test.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Maps;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.solr.client.solrj.response.RangeFacet;
import org.assertj.core.api.Assertions;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.junit.After;
import org.junit.BeforeClass;
import org.opensilex.integration.test.AbstractIntegrationTest;
import org.opensilex.security.SecurityModule;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.AuthenticationService;
import org.opensilex.security.authentication.api.AuthenticationAPI;
import org.opensilex.security.authentication.api.AuthenticationDTO;
import org.opensilex.security.authentication.api.TokenGetDTO;
import org.opensilex.server.response.JsonResponse;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.response.NamedResourceDTO;
import org.opensilex.sparql.response.ResourceDTO;
import org.opensilex.sparql.response.ResourceTreeDTO;
import org.opensilex.sparql.response.ResourceTreeResponse;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.service.SPARQLServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.*;

import static junit.framework.TestCase.assertEquals;
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

    /**
     * Call the security service and return a new Token
     *
     * @param userMail The user identifier
     * @param userPassword The user password
     * @return a new Token
     * @throws Exception in case of error during token retrieval
     */
    protected TokenGetDTO queryToken(String userMail, String userPassword) throws Exception {

        AuthenticationDTO authDto = new AuthenticationDTO();
        authDto.setIdentifier(userMail);
        authDto.setPassword(userPassword);

        final Response callResult = target("/security/authenticate")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(authDto, MediaType.APPLICATION_JSON_TYPE));

        JsonNode node = callResult.readEntity(JsonNode.class);

        // need to convert according a TypeReference, because the expected SingleObjectResponse is a generic object
        Object json = mapper.readValue(node.toString(), Object.class);
        if (callResult.getStatus() == Response.Status.OK.getStatusCode()) {
            SingleObjectResponse<TokenGetDTO> res = mapper.convertValue(node, new TypeReference<SingleObjectResponse<TokenGetDTO>>() {
            });
            return res.getResult();
        }

        throw new Exception("Error while getting token: " + json);
    }

    /**
     * Calls the security service and return a new token for the admin
     *
     * @return a new token for the admin
     * @throws Exception
     */
    protected TokenGetDTO queryAdminToken() throws Exception {
        return queryToken(ADMIN_MAIL, ADMIN_PASSWORD);
    }

    /**
     * Register the admin token in the token map for usage in tests.
     *
     * @throws Exception
     */
    protected void registerAdminTokenIfNecessary() throws Exception {
        if (!tokenMap.containsKey(ADMIN_MAIL)) {
            tokenMap.put(ADMIN_MAIL, queryAdminToken());
        }
    }

    /**
     * Register a token in the token map for usage in tests. Necessary to use before calling {@link #appendToken(WebTarget, String)}.
     *
     * @param userMail The user identifier
     * @param userPassword The user password
     * @throws Exception
     */
    protected void registerToken(String userMail, String userPassword) throws Exception {
        tokenMap.put(userMail, queryToken(userMail, userPassword));
    }

    /**
     * Get {@link Response} from an {@link ApiProtected} POST service call as admin.
     *
     * @param target the {@link WebTarget} on which POST the given entity
     * @param entity the data to POST on the given target
     * @return target invocation response.
     * @throws Exception in case of error during token retrieval
     */
    protected Response getJsonPostResponseAsAdmin(WebTarget target, Object entity) throws Exception {
        return appendAdminToken(target).post(Entity.entity(entity, MediaType.APPLICATION_JSON_TYPE));
    }

    /**
     * Get {@link Response} from an {@link ApiProtected} POST service call as admin.
     *
     * @param target the {@link WebTarget} on which POST the given entity
     * @param entity the data to POST on the given target
     * @return target invocation response with APPLICATION_OCTET_STREAM_TYPE {@link MediaType} as content
     * @throws Exception in case of error during token retrieval
     */
    protected Response getOctetPostResponseAsAdmin(WebTarget target, Object entity) throws Exception {
        return appendAdminToken(target)
                .accept(MediaType.APPLICATION_OCTET_STREAM_TYPE)
                .post(Entity.entity(entity, MediaType.APPLICATION_JSON_TYPE));
    }

    /**
     * Get {@link Response} from an {@link ApiProtected} POST service call. If you wish to perform the action using
     * the admin account, user the {@link #getJsonPostResponseAsAdmin(WebTarget, Object)} method.
     *
     * @param target the {@link WebTarget} on which POST the given entity
     * @param entity the data to POST on the given target
     * @param userMail the user identifier to perform the query
     * @return target invocation response.
     * @throws Exception in case of error during token retrieval
     */
    protected Response getJsonPostResponse(WebTarget target, Object entity, String userMail) throws Exception {
        return appendToken(target, userMail).post(Entity.entity(entity, MediaType.APPLICATION_JSON_TYPE));
    }

    /**
     * Get {@link Response} from an {@link ApiProtected} POST service call.
     *
     * @param target    the {@link WebTarget} on which POST the given entity
     * @param multipart the data to POST on the given target
     * @return target invocation response.
     * @throws Exception in case of error during token retrieval
     */
    protected Response getJsonPostResponseMultipart(WebTarget target, MultiPart multipart) throws Exception {
        return appendAdminToken(target.register(MultiPartFeature.class)).post(Entity.entity(multipart, MediaType.MULTIPART_FORM_DATA));
    }

    /**
     * Get {@link Response} from an {@link ApiProtected} POST service call as admin.
     *
     * @param target the {@link WebTarget} on which GET the given entity
     * @return target invocation response.
     * @throws Exception in case of error during token retrieval
     */
    protected Response getJsonGetResponseAsAdmin(WebTarget target) throws Exception {
        return appendAdminToken(target).get();
    }

    /**
     * Get {@link Response} from an {@link ApiProtected} POST service call.
     *
     * @param target the {@link WebTarget} on which GET the given entity
     * @param userMail the user identifier to perform the query
     * @return target invocation response.
     * @throws Exception in case of error during token retrieval
     */
    protected Response getJsonGetResponse(WebTarget target, String userMail) throws Exception {
        return appendToken(target, userMail).get();
    }

    /**
     * Get {@link Response} from an {@link ApiProtected} PUT service call.
     *
     * @param target    the {@link WebTarget} on which PUT the given entity
     * @param multipart the data to PUT on the given target
     * @return target invocation response.
     * @throws Exception in case of error during token retrieval
     */
    protected Response getJsonPutResponseMultipart(WebTarget target, MultiPart multipart) throws Exception {
        return appendAdminToken(target.register(MultiPartFeature.class)).put(Entity.entity(multipart, MediaType.MULTIPART_FORM_DATA));
    }

    /**
     * Get {@link Response} from an {@link ApiProtected} PUT service call.
     *
     * @param target the {@link WebTarget} on which PUT the given entity
     * @param entity the data to PUT on the given target
     * @return target invocation response.
     * @throws Exception in case of error during token retrieval
     */
    protected Response getJsonPutResponse(WebTarget target, Object entity) throws Exception {
        return appendAdminToken(target).put(Entity.entity(entity, MediaType.APPLICATION_JSON_TYPE));
    }

    /**
     * Get {@link Response} from a {@link ApiProtected} GET{uri} service call as admin.
     *
     * @param target the {@link WebTarget} on which get an entity with the given URI
     * @param uri    the URI of the resource to fetch from the given target.
     * @return target invocation response.
     * @throws Exception in case of error during token retrieval
     */
    protected Response getJsonGetByUriResponseAsAdmin(WebTarget target, String uri) throws Exception {
        return appendAdminToken(target.resolveTemplate("uri", uri)).get();
    }

    /**
     * Get {@link Response} from a {@link ApiProtected} GET{uri} service call.
     *
     * @param target the {@link WebTarget} on which get an entity with the given URI
     * @param uri    the URI of the resource to fetch from the given target.
     * @param userMail the user identifier to perform the query.
     * @return target invocation response.
     * @throws Exception in case of error during token retrieval
     */
    protected Response getJsonGetByUriResponse(WebTarget target, String uri, String userMail) throws Exception {
        return appendToken(target.resolveTemplate("uri", uri), userMail).get();
    }

    /**
     * Get {@link Response} from a {@link ApiProtected} GET{uri} service call.
     *
     * @param target the {@link WebTarget} on which get an entity with the given URI
     * @param uri    the URI of the resource to fetch from the given target.
     * @return target invocation response with APPLICATION_OCTET_STREAM_TYPE {@link MediaType} as content
     * @throws Exception in case of error during token retrieval
     */
    protected Response getOctetStreamByUriResponse(WebTarget target, String uri) throws Exception {
        return appendAdminToken(target.resolveTemplate("uri", uri))
                .accept(MediaType.APPLICATION_OCTET_STREAM_TYPE)
                .get();
    }

    /**
     * Get {@link Response} from a {@link ApiProtected} DELETE{uri} service call.
     *
     * @param target the {@link WebTarget} on which DELETE the given uri
     * @param uri    the URI of the resource to DELETE
     * @return target invocation response.
     * @throws Exception in case of error during token retrieval
     */
    protected Response getDeleteByUriResponse(WebTarget target, String uri) throws Exception {
        return appendAdminToken(target.resolveTemplate("uri", uri)).delete();
    }

    /**
     * Get {@link Response} from a public DELETE service call.
     *
     * @param target the {@link WebTarget} on which DELETE some content
     * @return target invocation response.
     * @throws Exception in case of error during token retrieval
     */
    protected Response getDeleteJsonResponse(WebTarget target) throws Exception {
        return appendAdminToken(target).delete();
    }

    private static final String ADMIN_MAIL = "admin@opensilex.org";
    private static final String ADMIN_PASSWORD = "admin";
    private final HashMap<String, TokenGetDTO> tokenMap = new HashMap<>();

    /**
     * Append a token header for the admin user to the given {@link WebTarget}
     *
     * @param target the {@link WebTarget} on which append params
     * @return the updated {@link WebTarget}
     * @throws Exception in case of error during token retrieval
     */
    protected Invocation.Builder appendAdminToken(WebTarget target) throws Exception {
        registerAdminTokenIfNecessary();

        return appendToken(target, ADMIN_MAIL);
    }

    /**
     * Append a token header to the give {@link WebTarget}. The token must first be registered using the method {@link #registerToken(String, String)}
     * before your test.
     *
     * @param target the {@link WebTarget} on which append params
     * @param userMail the use identifier to perform the query
     * @return the updated {@link WebTarget}
     * @throws IllegalArgumentException if a token has not been registered for this user yet
     */
    protected Invocation.Builder appendToken(WebTarget target, String userMail) throws IllegalArgumentException {
        if (!tokenMap.containsKey(userMail)) {
            throw new IllegalArgumentException("Cannot find a token for user " + userMail + ". Please generate a token using `registerToken` before your test.");
        }

        return target.request(MediaType.APPLICATION_JSON_TYPE)
                .header(ApiProtected.HEADER_NAME, ApiProtected.TOKEN_PARAMETER_PREFIX + tokenMap.get(userMail).getToken());
    }

    /**
     * Call the createPath with the given entity, check if has been created, delete it and then check that the resource has been deleted
     *
     * @param getByUriPath the path to the service which allow to fetch an entity by it's URI
     * @param createPath   the path to the service which allow to create an entity
     * @param deletePath   the path to the service which allow to delete an entity
     * @param entity       the entity on which apply create, read and delete
     */
    protected void testCreateGetAndDelete(String createPath, String getByUriPath, String deletePath, Object entity) throws Exception {

        final Response postResult = getJsonPostResponseAsAdmin(target(createPath), entity);
        assertEquals(Response.Status.CREATED.getStatusCode(), postResult.getStatus());

        // ensure that the result is a well formed URI, else throw exception
        String uri = extractUriFromResponse(postResult).toString();

        Response getResult = getJsonGetByUriResponseAsAdmin(target(getByUriPath), uri);
        assertEquals(Response.Status.OK.getStatusCode(), getResult.getStatus());

        // delete object and check if URI no longer exists
        final Response delResult = getDeleteByUriResponse(target(deletePath), uri);
        assertEquals(Response.Status.OK.getStatusCode(), delResult.getStatus());

        getResult = getJsonGetByUriResponseAsAdmin(target(getByUriPath), uri);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), getResult.getStatus());
    }

    /**
     * Call the createPath with the given entity list
     * then for each entity : check if entity has been created, delete it and then check that the resource has been deleted
     *
     * @param getByUriPath the path to the service which allow to fetch an entity by it's URI
     * @param createPath   the path to the service which allow to create an entity
     * @param deletePath   the path to the service which allow to delete an entity
     * @param entities     the List of entity on which apply create, read and delete
     */
    protected void testCreateListGetAndDelete(String createPath, String getByUriPath, String deletePath, List<Object> entities) throws Exception {

        final Response postResult = getJsonPostResponseAsAdmin(target(createPath), entities);
        assertEquals(Response.Status.CREATED.getStatusCode(), postResult.getStatus());

        List<URI> uris = extractUriListFromPaginatedListResponse(postResult);

        for (URI uri : uris) {
            Response getResult = getJsonGetByUriResponseAsAdmin(target(getByUriPath), uri.toString());
            assertEquals(Response.Status.OK.getStatusCode(), getResult.getStatus());

            // delete object and check if URI no longer exists
            final Response delResult = getDeleteByUriResponse(target(deletePath), uri.toString());
            assertEquals(Response.Status.OK.getStatusCode(), delResult.getStatus());

            getResult = getJsonGetByUriResponseAsAdmin(target(getByUriPath), uri.toString());
            assertEquals(Response.Status.NOT_FOUND.getStatusCode(), getResult.getStatus());
        }

    }


    protected <T> List<T> getSearchResultsAsAdmin(String searchPath, Integer page, Integer pageSize, Map<String, Object> searchCriteria, TypeReference<PaginatedListResponse<T>> typeReference) throws Exception {
        registerAdminTokenIfNecessary();
        return getSearchResults(searchPath, page, pageSize, searchCriteria, typeReference, ADMIN_MAIL);
    }

    protected <T> List<T> getSearchResults(String searchPath, Integer page, Integer pageSize, Map<String, Object> searchCriteria, TypeReference<PaginatedListResponse<T>> typeReference, String userMail) throws Exception {
        if (searchCriteria == null) {
            searchCriteria = new HashMap<>();
        }
        WebTarget searchTarget = appendSearchParams(target(searchPath), page, pageSize, searchCriteria);
        final Response getResult = appendToken(searchTarget, userMail).get();
        assertEquals(Response.Status.OK.getStatusCode(), getResult.getStatus());

        return readResponse(getResult, typeReference).getResult();
    }

    protected <T> List<T> getSearchResultsAsAdmin(String searchPath, Map<String, Object> searchCriteria, TypeReference<PaginatedListResponse<T>> typeReference) throws Exception {
        return this.getSearchResultsAsAdmin(searchPath, 0, 20, searchCriteria, typeReference);
    }

    protected <T> List<T> getSearchResults(String searchPath, Map<String, Object> searchCriteria, TypeReference<PaginatedListResponse<T>> typeReference, String userMail) throws Exception {
        return this.getSearchResults(searchPath, 0, 20, searchCriteria, typeReference, userMail);
    }

    protected List<ResourceTreeDTO> getTreeResults(String searchPath, Map<String, Object> searchCriteria) throws Exception {
        if (searchCriteria == null) {
            searchCriteria = new HashMap<>();
        }
        WebTarget searchTarget = appendQueryParams(target(searchPath), searchCriteria);
        final Response getResult = appendAdminToken(searchTarget).get();
        assertEquals(Response.Status.OK.getStatusCode(), getResult.getStatus());

        return readResponse(getResult, new TypeReference<ResourceTreeResponse>() {}).getResult();
    }

    public static boolean nonNullAttributesInclusionComparison(Object superObject, Object subObject) {
        LinkedHashMap<String, Object> superMap = convertToNestedMap(superObject);
        LinkedHashMap<String, Object> subMap = convertToNestedMap(subObject);
        return isDeepMapIncluded(superMap, subMap);
    }

    public static boolean isDeepMapIncluded(LinkedHashMap<String, Object> superMap, LinkedHashMap<String, Object> subMap) {
        for (Map.Entry<String, Object> entry : subMap.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            Object correspondingValue = superMap.get(key);

            if (value instanceof Map && correspondingValue instanceof Map) {
                if (!isDeepMapIncluded((LinkedHashMap<String, Object>) value, (LinkedHashMap<String, Object>) correspondingValue)) {
                    return false;
                }
            } else if (value instanceof List) {
                for (LinkedHashMap v1 : (List<LinkedHashMap>)value) {
                    for (LinkedHashMap v2 : (List<LinkedHashMap>)correspondingValue) {
                        if (isDeepMapIncluded(v1, v2)) {
                            return true;
                        }
                    }
                }
            } else if (!superMap.containsKey(key) || !superMap.get(key).equals(value)) {
                return false;
            }
        }
        return true;
    }

    protected static LinkedHashMap<String, Object> convertToNestedMap(Object object) {
        // Convert the object to a map
        LinkedHashMap<String, Object> map = mapper.convertValue(object, LinkedHashMap.class);
        map.values().removeAll(Collections.singleton(null));

        // Recursively convert nested objects to nested maps of maps
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (
                    !(entry.getValue() instanceof String) &&
                            !(entry.getValue() instanceof Integer) &&
                            !(entry.getValue() instanceof Short) &&
                            !(entry.getValue() instanceof Long) &&
                            !(entry.getValue() instanceof Float) &&
                            !(entry.getValue() instanceof Double) &&
                            !(entry.getValue() instanceof Boolean)
            ) {
                Object nestedObject = entry.getValue();
                if (Objects.isNull(nestedObject)) {
                    map.remove(entry.getKey());
                } else if (nestedObject instanceof List) {
                    List<Object> nestedList = new ArrayList<>();
                    for (Object value : (List<Object>) nestedObject) {
                        nestedList.add(convertToNestedMap(value));
                    }
                    if (nestedList.isEmpty()) {
                        map.remove(entry.getKey());
                    } else {
                        entry.setValue(nestedList);
                    }
                } else {
                    LinkedHashMap<String, Object> nestedMap = convertToNestedMap(nestedObject);
                    if (nestedMap.isEmpty()) {
                        map.remove(entry.getKey());
                    } else {
                        entry.setValue(nestedMap);
                    }
                }
            }
        }

        return map;
    }

    protected <T> void testBasicCRUDAsAdmin(
            ServiceDescription createServiceDescription,
            ServiceDescription readServiceDescription,
            ServiceDescription updateServiceDescription,
            ServiceDescription deleteServiceDescription,
            NamedResourceDTO<?> entityToPost, NamedResourceDTO<?> entityToPut,
            LinkedHashMap<String, Object> attributesToCheck, TypeReference<SingleObjectResponse<T>> entityTypeReference
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
        // Get the updated object
        SingleObjectResponse<T> readResponse = readCall.executeCallAndDeserialize(entityTypeReference).getDeserializedResponse();
        LinkedHashMap<String, Object> responseAttributes = mapper.convertValue(readResponse.getResult(), LinkedHashMap.class);
        // Check attributes value
        assertTrue(isDeepMapIncluded(responseAttributes, attributesToCheck));

        // DELETE
        UserCall deleteCall = new UserCallBuilder(deleteServiceDescription)
                .setUriInPath(createdUri.toString())
                .buildAdmin();
        deleteCall.executeCallAndReturnURI();
        Response result = readCall.executeCall();
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), result.getStatus());
    }

    protected <T> void testBasicCRUDListAsAdmin(
            ServiceDescription createServiceDescription,
            ServiceDescription readServiceDescription,
            ServiceDescription updateServiceDescription,
            ServiceDescription deleteServiceDescription,
            List<NamedResourceDTO<?>> entitiesToPost, List<NamedResourceDTO<?>> entitiesToPut,
            List<LinkedHashMap<String, Object>> attributesToCheck, TypeReference<SingleObjectResponse<T>> entitiesTypeReference
    ) throws Exception {
        if (entitiesToPost.size() != entitiesToPut.size()) {
            for (int i = 0; i < entitiesToPost.size(); i++) {
                testBasicCRUDAsAdmin(
                        createServiceDescription,
                        readServiceDescription,
                        updateServiceDescription,
                        deleteServiceDescription,
                        entitiesToPost.get(i), entitiesToPut.get(i),
                        attributesToCheck.get(i), entitiesTypeReference
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
            this.httpMethod = findHttpMethod(serviceMethod);
        }

        @Override
        public Response executeCall() throws Exception {
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
            if (Objects.equals(httpMethod, HttpMethod.PUT) ||
                    Objects.equals(httpMethod, HttpMethod.POST) ||
                    Objects.equals(httpMethod, HttpMethod.DELETE)) {
                Result<ObjectUriResponse> response = executeCallAndDeserialize(new TypeReference<ObjectUriResponse>() {
                });
                return URI.create(response.getDeserializedResponse().getResult());
            } else {
                Result<SPARQLResourceModel> readResponse = executeCallAndDeserialize(new TypeReference<SPARQLResourceModel>() {
                });
                return readResponse.getDeserializedResponse().getUri();
            }
        }

        @Override
        protected Response makeCorrectCall(WebTarget target, String httpMethod, Object body, MediaType callMediaType, List<MediaType> responseMediaTypes) {

            Invocation.Builder requestBuilder;
            if (Objects.isNull(callMediaType)) {
                requestBuilder = target.request(MediaType.APPLICATION_JSON);
            } else {
                requestBuilder = target.request(callMediaType);
            }

            if (!(responseMediaTypes == null) && !responseMediaTypes.isEmpty()) {
                for (MediaType mediaType : responseMediaTypes) {
                    requestBuilder = requestBuilder.accept(mediaType);
                }
            }

            requestBuilder = requestBuilder.header(ApiProtected.HEADER_NAME, ApiProtected.TOKEN_PARAMETER_PREFIX + tokenMap.get(userEmail).getToken());

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

        protected void authenticateAndRegisterIfNecessary(String userEmail, String userPassword) throws Exception {

            if (!tokenMap.containsKey(userEmail)){
                AuthenticationDTO authDto = new AuthenticationDTO();
                authDto.setIdentifier(userEmail);
                authDto.setPassword(userPassword);

                PublicCall tokenCall = new PublicCallBuilder<>(authenticate).setBody(authDto).build();

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

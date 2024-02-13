//******************************************************************************
//                          AbstractIntegrationTest.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.integration.test.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
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
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.response.ResourceDTO;
import org.opensilex.sparql.response.ResourceTreeDTO;
import org.opensilex.sparql.response.ResourceTreeResponse;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.service.SPARQLServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.lang.reflect.Method;
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
    @Deprecated
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
    @Deprecated
    protected TokenGetDTO queryAdminToken() throws Exception {
        return queryToken(ADMIN_MAIL, ADMIN_PASSWORD);
    }

    /**
     * Register the admin token in the token map for usage in tests.
     *
     * @throws Exception
     */
    @Deprecated
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
    @Deprecated
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
    @Deprecated
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
    @Deprecated
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
    @Deprecated
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
    @Deprecated
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
    @Deprecated
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
    @Deprecated
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
    @Deprecated
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
    @Deprecated
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
    @Deprecated
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
    @Deprecated
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
    @Deprecated
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
    @Deprecated
    protected Response getDeleteByUriResponse(WebTarget target, String uri) throws Exception {
        return appendAdminToken(target.resolveTemplate("uri", uri)).delete();
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
    @Deprecated
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
    @Deprecated
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
     * @param getByUriPath the path to the service which allow to fetch an entity by its URI
     * @param createPath   the path to the service which allow to create an entity
     * @param deletePath   the path to the service which allow to delete an entity
     * @param entity       the entity to create, read and delete
     */
    @Deprecated
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
     * @param getByUriPath the path to the service which allow to fetch an entity by its URI
     * @param createPath   the path to the service which allow to create an entity
     * @param deletePath   the path to the service which allow to delete an entity
     * @param entities     the List of entities to create, read and delete
     */
    @Deprecated
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

    @Deprecated
    protected <T> List<T> getSearchResultsAsAdmin(String searchPath, Integer page, Integer pageSize, Map<String, Object> searchCriteria, TypeReference<PaginatedListResponse<T>> typeReference) throws Exception {
        registerAdminTokenIfNecessary();
        return getSearchResults(searchPath, page, pageSize, searchCriteria, typeReference, ADMIN_MAIL);
    }

    @Deprecated
    protected <T> List<T> getSearchResults(String searchPath, Integer page, Integer pageSize, Map<String, Object> searchCriteria, TypeReference<PaginatedListResponse<T>> typeReference, String userMail) throws Exception {
        if (searchCriteria == null) {
            searchCriteria = new HashMap<>();
        }
        WebTarget searchTarget = appendSearchParams(target(searchPath), page, pageSize, searchCriteria);
        final Response getResult = appendToken(searchTarget, userMail).get();
        assertEquals(Response.Status.OK.getStatusCode(), getResult.getStatus());

        return readResponse(getResult, typeReference).getResult();
    }

    @Deprecated
    protected <T> List<T> getSearchResultsAsAdmin(String searchPath, Map<String, Object> searchCriteria, TypeReference<PaginatedListResponse<T>> typeReference) throws Exception {
        return this.getSearchResultsAsAdmin(searchPath, 0, 20, searchCriteria, typeReference);
    }

    @Deprecated
    protected <T> List<T> getSearchResults(String searchPath, Map<String, Object> searchCriteria, TypeReference<PaginatedListResponse<T>> typeReference, String userMail) throws Exception {
        return this.getSearchResults(searchPath, 0, 20, searchCriteria, typeReference, userMail);
    }

    @Deprecated
    protected List<ResourceTreeDTO> getTreeResults(String searchPath, Map<String, Object> searchCriteria) throws Exception {
        if (searchCriteria == null) {
            searchCriteria = new HashMap<>();
        }
        WebTarget searchTarget = appendQueryParams(target(searchPath), searchCriteria);
        final Response getResult = appendAdminToken(searchTarget).get();
        assertEquals(Response.Status.OK.getStatusCode(), getResult.getStatus());

        return readResponse(getResult, new TypeReference<ResourceTreeResponse>() {}).getResult();
    }

    /**
     * This method checks if all elements of a nested Map (subMap) are included in another nested Map (superMap).
     * @param superMap The Map that is supposed to contain all elements of the subMap.
     * @param subMap The Map whose elements are checked if they are contained in the superMap.
     * @return returns true if all elements of the subMap are included in the superMap, otherwise it returns false.
     */
    public static boolean isDeepMapIncluded(LinkedHashMap<String, Object> superMap, LinkedHashMap<String, Object> subMap) {
        for (Map.Entry<String, Object> entry : subMap.entrySet()) {

            String key = entry.getKey();
            // Case key not in superMap
            if (!superMap.containsKey(key)){
                return false;
            }
            Object value = entry.getValue();
            Object correspondingValue = superMap.get(key);

            // Case recursion on internal Map returns false
            if (value instanceof Map && correspondingValue instanceof Map) {
                if (!isDeepMapIncluded((LinkedHashMap<String, Object>) value, (LinkedHashMap<String, Object>) correspondingValue)) {
                    return false;
                }
            }

            // Case List elements not included
            else if (value instanceof List && correspondingValue instanceof List) {
                if (!isDeepListIncluded((List<Object>)value, (List<Object>) correspondingValue)){
                    return false;
                }
            }

            // Case primitives aren't equal
            if (!Objects.equals(value, correspondingValue)) {

                equalityFound:
                {
                    // Case String representations of URIs equals
                    if (value instanceof String && superMap.get(key) instanceof String) {
                        try {
                            if (SPARQLDeserializers.compareURIs((String)value, (String)correspondingValue)) {
                                break equalityFound;
                            }
                        } catch (Exception ignored) {

                        }
                    }

                    // Case no equality found
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * This method checks if all elements of a nested List (subList) are included in another nested List (superList).
     * @param superList The List that is supposed to contain all elements of the subList.
     * @param subList The List whose elements are checked if they are contained in the superList.
     * @return returns true if all elements of the subList are included in the superList, otherwise it returns false.
     */
    public static boolean isDeepListIncluded(List<Object> superList, List<Object> subList) {
        for (Object subElement : subList) {
            found:
            {
                for (Object superElement : superList) {
                    // case elements are equal (handles all primitives)
                    if (Objects.equals(subElement, superElement)) {
                        break found;
                    }
                    // case elements are Maps and the subElement is included in the superElement
                    else if (subElement instanceof Map && superElement instanceof Map) {
                        if (isDeepMapIncluded((LinkedHashMap<String, Object>) subElement,
                                (LinkedHashMap<String, Object>) superElement)) {
                            break found;
                        }
                    }
                    // case elements are Lists and the subElement is included in the superElement
                    else if (subElement instanceof List && superElement instanceof List) {
                        if (isDeepListIncluded((List) subElement,
                                (List) superElement)) {
                            break found;
                        }
                    }
                    // case elements are string representations of URIs and are equals
                    else if (subElement instanceof String && superElement instanceof String) {
                        try {
                            return SPARQLDeserializers.compareURIs((String)subElement, (String)superElement);
                        } catch (Exception ignored) {

                        }
                    }
                }
                return false;
            }
        }
        return true;
    }

    /**
     * This method converts a given object into a nested map, and removes all null and empty values.
     * @param object The object to be converted into a map.
     * @return returns a LinkedHashMap where the object has been converted into a map and all null values have been removed.
     */
    public static LinkedHashMap<String, Object> convertToNotNullNestedMap(Object object) {
        // Convert the object to a map
        LinkedHashMap<String, Object> map = mapper.convertValue(object, LinkedHashMap.class);
        map.values().removeAll(Collections.singleton(null));

        List<String> keysToRemove = new ArrayList<>();
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
                        if (value instanceof String ||
                                value instanceof Integer ||
                                value instanceof Short ||
                                value instanceof Long ||
                                value instanceof Float ||
                                value instanceof Double ||
                                value instanceof Boolean) {
                            nestedList.add(value);
                        } else {
                            nestedList.add(convertToNotNullNestedMap(value));
                        }
                    }
                    if (nestedList.isEmpty()) {
                        keysToRemove.add(entry.getKey());
                    } else {
                        entry.setValue(nestedList);
                    }
                } else {
                    LinkedHashMap<String, Object> nestedMap = convertToNotNullNestedMap(nestedObject);
                    if (nestedMap.isEmpty()) {
                        keysToRemove.add(entry.getKey());
                    } else {
                        entry.setValue(nestedMap);
                    }
                }
            }
        }
        for (String key:keysToRemove) {
            map.remove(key);
        }

        return map;
    }

    /**
     * This method simulates a basic CREATE operation as an administrator.
     * @param createServiceDescription The service description of the create operation.
     * @param entityToPost The entity to be posted.
     * @return returns the URI of the created resource.
     * @throws Exception if the operation fails.
     */
    protected URI testBasicCreateAsAdmin(
            ServiceDescription createServiceDescription,
            ResourceDTO<?> entityToPost
    ) throws Exception {
        return new UserCallBuilder(createServiceDescription)
                .setBody(entityToPost)
                .buildAdmin()
                .executeCallAndReturnURI();
    }

    /**
     * This method tests a basic READ operation as an administrator.
     * @param readServiceDescription The service description of the read operation.
     * @param entityTypeReference The type reference of the entity to be read.
     * @param resourceUri The URI of the resource to be read.
     * @return returns the read entity.
     * @throws Exception if the operation fails.
     */
    protected <T extends JsonResponse<?>> T testBasicReadAsAdmin(
            ServiceDescription readServiceDescription,
            TypeReference<T> entityTypeReference,
            URI resourceUri
    ) throws Exception {
        return new UserCallBuilder(readServiceDescription)
                .setUriInPath(resourceUri.toString())
                .buildAdmin()
                .executeCallAndDeserialize(entityTypeReference)
                .getDeserializedResponse();
    }

    /**
     * This method tests a basic UPDATE operation as an administrator. null or empty parameters aren't taken into account.
     * @param readServiceDescription The service description of the read operation.
     * @param updateServiceDescription The service description of the update operation.
     * @param entityToPut The entity for the update.
     * @param entityTypeReference The type reference of the entity to be updated.
     * @throws Exception if the operation fails.
     */
    protected <T extends JsonResponse<?>> void testBasicUpdateAsAdmin(
            ServiceDescription readServiceDescription,
            ServiceDescription updateServiceDescription,
            ResourceDTO<?> entityToPut,
            TypeReference<T> entityTypeReference
    ) throws Exception {
        new UserCallBuilder(updateServiceDescription)
                .setBody(entityToPut)
                .buildAdmin()
                .executeCallAndAssertStatus(Response.Status.OK);
        // Get the updated object
        T readResponse = testBasicReadAsAdmin(readServiceDescription, entityTypeReference, entityToPut.getUri());
        LinkedHashMap<String, Object> responseAttributes = convertToNotNullNestedMap(readResponse.getResult());
        LinkedHashMap<String, Object> expectedAttributes = convertToNotNullNestedMap(entityToPut);
        // Check attributes value
        assertTrue(isDeepMapIncluded(responseAttributes, expectedAttributes));
    }

    /**
     * This method tests a basic DELETE operation as an administrator.
     * @param readServiceDescription The service description of the read operation.
     * @param deleteServiceDescription The service description of the delete operation.
     * @param resourceUri The URI of the resource to be deleted.
     * @throws Exception if the operation fails.
     */
    protected void testBasicDeleteAsAdmin(
            ServiceDescription readServiceDescription,
            ServiceDescription deleteServiceDescription,
            URI resourceUri
    ) {
        new UserCallBuilder(deleteServiceDescription)
                .setUriInPath(resourceUri.toString())
                .buildAdmin()
                .executeCallAndAssertStatus(Response.Status.OK);
        new UserCallBuilder(readServiceDescription)
                .setUriInPath(resourceUri.toString())
                .buildAdmin()
                .executeCallAndAssertStatus(Response.Status.NOT_FOUND);
    }

    /**
     * This method tests the basic CRUD operations as an administrator.
     * null or empty parameters aren't taken into account to check the update step.
     * @param createServiceDescription The service description of the create operation.
     * @param readServiceDescription The service description of the read operation.
     * @param updateServiceDescription The service description of the update operation.
     * @param deleteServiceDescription The service description of the delete operation.
     * @param entityToPost The entity to be posted.
     * @param entityToPut The entity for the update.
     * @param entityTypeReference The type reference of the entity to be manipulated.
     * @throws Exception if any of the operations fail.
     */
    protected <T extends JsonResponse<?>> void testBasicCRUDAsAdmin(
            ServiceDescription createServiceDescription,
            ServiceDescription readServiceDescription,
            ServiceDescription updateServiceDescription,
            ServiceDescription deleteServiceDescription,
            ResourceDTO<?> entityToPost, ResourceDTO<?> entityToPut,
            TypeReference<T> entityTypeReference
    ) throws Exception {
        // CREATE
        URI createdUri = testBasicCreateAsAdmin(createServiceDescription, entityToPost);
        // READ
        testBasicReadAsAdmin(readServiceDescription, entityTypeReference, createdUri);
        // UPDATE
        entityToPut.setUri(createdUri);
        testBasicUpdateAsAdmin(readServiceDescription, updateServiceDescription, entityToPut, entityTypeReference);
        // DELETE
        testBasicDeleteAsAdmin(readServiceDescription, deleteServiceDescription, createdUri);
    }

    protected TokenGetDTO authenticateAndRegisterIfNecessary(String userEmail, String userPassword) throws Exception {

        if (!tokenMap.containsKey(userEmail)){
            AuthenticationDTO authDto = new AuthenticationDTO();
            authDto.setIdentifier(userEmail);
            authDto.setPassword(userPassword);

            PublicCall tokenCall = new PublicCallBuilder<>(authenticate).setBody(authDto).build();

            Result<SingleObjectResponse<TokenGetDTO>> postResult = tokenCall.executeCallAndDeserialize(
                    new TypeReference<SingleObjectResponse<TokenGetDTO>>() {}
            );
            TokenGetDTO userToken = postResult.getDeserializedResponse().getResult();
            tokenMap.put(userEmail, userToken);
        }
        return tokenMap.get(userEmail);
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
            this.httpMethod = findHttpMethod();
        }

        /**
         * Executes the call and returns the raw response.
         * @return the response of the call.
         * @throws Exception if there is an error executing the call
         */
        @Override
        public Response executeCall() throws Exception {
            WebTarget target = createTarget();
            authenticateAndRegisterIfNecessary(userEmail, userPassword);
            appendToken(target, userEmail);
            return makeCorrectCall(target);
        }

        /**
         * Makes the correct call based on the target.
         * @param target the target for the call
         * @return the response of the call
         */
        @Override
        protected Response makeCorrectCall(WebTarget target) {
            Invocation.Builder requestBuilder = buildRequestBuilder(target);
            requestBuilder = requestBuilder.header(ApiProtected.HEADER_NAME, ApiProtected.TOKEN_PARAMETER_PREFIX + tokenMap.get(userEmail).getToken());
            return executeRequest(requestBuilder);
        }

        @Override
        public String toString() {
            return "UserCall{" +
                    "userEmail='" + userEmail + '\'' +
                    ", userPassword='" + userPassword + '\'' +
                    ", params=" + params +
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

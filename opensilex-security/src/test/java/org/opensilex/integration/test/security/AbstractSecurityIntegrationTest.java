//******************************************************************************
//                          AbstractIntegrationTest.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.integration.test.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;

import java.net.URI;
import java.util.ArrayList;

import org.junit.After;
import org.junit.Assert;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.opensilex.integration.test.AbstractIntegrationTest;
import org.opensilex.security.SecurityModule;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.AuthenticationService;
import org.opensilex.security.authentication.api.AuthenticationDTO;
import org.opensilex.security.authentication.api.TokenGetDTO;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.response.ResourceTreeDTO;
import org.opensilex.sparql.response.ResourceTreeResponse;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.service.SPARQLServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

import static junit.framework.TestCase.assertEquals;


/**
 * @author Vincent MIGOT
 * <p>
 * Abstract class used for Secure API testing
 */
public abstract class AbstractSecurityIntegrationTest extends AbstractIntegrationTest {

    protected final static Logger LOGGER = LoggerFactory.getLogger(AbstractSecurityIntegrationTest.class);

    protected static SecurityModule securityModule;

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

        JsonNode node = getResult.readEntity(JsonNode.class);
        Assert.assertNull(node.get("resultWithPagination"));
        Assert.assertNotNull(node.get("result"));

        PaginatedListResponse<T> responseList = mapper.convertValue(node, typeReference);
        return responseList.getResult();
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

        JsonNode node = getResult.readEntity(JsonNode.class);
        return mapper.convertValue(node, new TypeReference<ResourceTreeResponse>() {
        }).getResult();
    }

}

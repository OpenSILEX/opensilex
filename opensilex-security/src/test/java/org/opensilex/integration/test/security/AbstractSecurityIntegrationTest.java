//******************************************************************************
//                          AbstractIntegrationTest.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.integration.test.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.ArrayList;
import org.junit.After;
import org.opensilex.server.response.SingleObjectResponse;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import javax.ws.rs.core.HttpHeaders;
import org.junit.BeforeClass;
import org.opensilex.OpenSilex;
import org.opensilex.integration.test.AbstractIntegrationTest;
import org.opensilex.security.SecurityModule;
import org.opensilex.security.authentication.ApiProtected;
import org.opensilex.security.authentication.AuthenticationService;
import org.opensilex.security.authentication.api.AuthenticationDTO;
import org.opensilex.security.authentication.api.TokenGetDTO;
import org.opensilex.sparql.exceptions.SPARQLQueryException;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.service.SPARQLServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Vincent MIGOT
 *
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

    /**
     * Clear the list of SPARQL graph to clear after each test execution
     *
     * @see IntegrationTestContext#clearGraphs(List)
     * @see #getGraphsToCleanNames()
     */
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
     * @throws SPARQLQueryException if an errors occurs during SPARQL query execution
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
     * @return the List of SPARQL Model to clear after each test execution.
     */
    protected List<Class<? extends SPARQLResourceModel>> getModelsToClean() {
        return new ArrayList<>();
    }

    /**
     * Call the security service and return a new Token
     *
     * @return a new Token
     */
    protected TokenGetDTO getToken() throws Exception {

        AuthenticationDTO authDto = new AuthenticationDTO();
        authDto.setIdentifier("admin@opensilex.org");
        authDto.setPassword("admin");

        final Response callResult = target("/security/authenticate")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(authDto, MediaType.APPLICATION_JSON_TYPE));

        JsonNode node = callResult.readEntity(JsonNode.class);

        // need to convert according a TypeReference, because the expected SingleObjectResponse is a generic object
        ObjectMapper mapper = new ObjectMapper();
        Object json = mapper.readValue(node.toString(), Object.class);
        if (callResult.getStatus() == Response.Status.OK.getStatusCode()) {
            SingleObjectResponse<TokenGetDTO> res = mapper.convertValue(node, new TypeReference<SingleObjectResponse<TokenGetDTO>>() {
            });
            return res.getResult();
        }

        throw new Exception("Error while getting token: " + json);
    }

    /**
     *
     * Get {@link Response} from an {@link ApiProtected} POST service call.
     *
     * @param target the {@link WebTarget} on which POST the given entity
     * @param entity the data to POST on the given target
     * @return target invocation response.
     */
    protected Response getJsonPostResponse(WebTarget target, Object entity) throws Exception {
        return appendToken(target).post(Entity.entity(entity, MediaType.APPLICATION_JSON_TYPE));
    }

    /**
     *
     * Get {@link Response} from an {@link ApiProtected} POST service call.
     *
     * @param target the {@link WebTarget} on which POST the given entity
     * @param entity the data to POST on the given target
     * @param lang the translation language of the requested data
     * @return target invocation response.
      */
    protected Response getJsonPostResponse(WebTarget target, Object entity, String lang) throws Exception {
        if(lang == null){
            return appendToken(target).header(HttpHeaders.ACCEPT_LANGUAGE, OpenSilex.DEFAULT_LANGUAGE).post(Entity.entity(entity, MediaType.APPLICATION_JSON_TYPE));
        }
        return appendToken(target).header(HttpHeaders.ACCEPT_LANGUAGE, lang).post(Entity.entity(entity, MediaType.APPLICATION_JSON_TYPE));
    }
    
    /**
     *
     * Get {@link Response} from an {@link ApiProtected} PUT service call.
     *
     * @param target the {@link WebTarget} on which PUT the given entity
     * @param entity the data to PUT on the given target
     * @return target invocation response.
     */
    protected Response getJsonPutResponse(WebTarget target, Object entity) throws Exception {
        return appendToken(target).put(Entity.entity(entity, MediaType.APPLICATION_JSON_TYPE));
    }

    /**
     *
     * Get {@link Response} from a {@link ApiProtected} GET{uri} service call.
     *
     * @param target the {@link WebTarget} on which get an entity with the given URI
     * @param uri the URI of the resource to fetch from the given target.
     * @return target invocation response.
     *
     * @see WebTarget#resolveTemplate(String, Object)
     */
    protected Response getJsonGetByUriResponse(WebTarget target, String uri) throws Exception {
        return appendToken(target.resolveTemplate("uri", uri)).get();
    }

    /**
     *
     * Get {@link Response} from a {@link ApiProtected} DELETE{uri} service call.
     *
     * @param target the {@link WebTarget} on which DELETE the given uri
     * @param uri the URI of the resource to DELETE
     *
     * @return target invocation response.
     *
     * @see WebTarget#resolveTemplate(String, Object)
     */
    protected Response getDeleteByUriResponse(WebTarget target, String uri) throws Exception {
        return appendToken(target.resolveTemplate("uri", uri)).delete();
    }

    /**
     *
     * Get {@link Response} from a public DELETE service call.
     *
     * @param target the {@link WebTarget} on which DELETE some content
     * @return target invocation response.
     *
     */
    protected Response getDeleteJsonResponse(WebTarget target) throws Exception {
        return appendToken(target).delete();
    }

    private TokenGetDTO token = null;

    /**
     * Append a token header to the given {@link WebTarget}
     *
     * @param target the {@link WebTarget} on which append params
     *
     * @return the updated {@link WebTarget}
     *
     * @see #getToken()
     * @see ApiProtected#HEADER_NAME
     * @see ApiProtected#TOKEN_PARAMETER_PREFIX
     */
    protected Invocation.Builder appendToken(WebTarget target) throws Exception {
        if (token == null) {
            token = getToken();
        }
        return target.request(MediaType.APPLICATION_JSON_TYPE)
                .header(ApiProtected.HEADER_NAME, ApiProtected.TOKEN_PARAMETER_PREFIX + token.getToken());
    }
}

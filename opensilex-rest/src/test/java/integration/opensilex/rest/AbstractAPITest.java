package integration.opensilex.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.After;
import org.junit.AfterClass;
import org.mockito.Mockito;
import org.opensilex.OpenSilex;
import org.opensilex.rest.RestApplication;
import org.opensilex.rest.authentication.ApiProtected;
import org.opensilex.rest.security.api.AuthenticationDTO;
import org.opensilex.rest.security.api.TokenGetDTO;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.sparql.exceptions.SPARQLQueryException;
import org.opensilex.sparql.utils.OrderBy;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.assertEquals;

/**
 * @author Renaud COLIN
 * Abstract class used for DAO testing
 */
public abstract class AbstractAPITest extends JerseyTest {

    protected static OpenSilexTestContext context;

    @Override
    protected ResourceConfig configure() {
        try {
            // init the OpenSilex instance to use during the API test(s)
            context = new OpenSilexTestContext();
        } catch (Exception e) {
          throw new RuntimeException(e);
        }

        ResourceConfig resourceConfig = new RestApplication(OpenSilex.getInstance());

        // create a mock for HttpServletRequest which is not available with grizzly
        final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        resourceConfig.register(new AbstractBinder() {
            @Override
            protected void configure() {
                bind(request).to(HttpServletRequest.class);
            }
        });

        resourceConfig.register(JacksonFeature.class);
        return resourceConfig;
    }

    @AfterClass
    public static void end() throws Exception {
        context.shutdown();
    }

    /**
     * @throws URISyntaxException
     * @throws SPARQLQueryException
     */
    @After
    public void clearGraph() throws URISyntaxException, SPARQLQueryException {
        context.clearGraphs(getGraphsToCleanNames());
    }

    /**
     * @return
     */
    protected List<String> getGraphsToCleanNames() {
        return new ArrayList<>();
    };


    protected static ObjectMapper mapper = new ObjectMapper();

    /**
     * Call the security service and return a new Token
     *
     * @return a new Token
     */
    protected TokenGetDTO getToken() {

        AuthenticationDTO authDto = new AuthenticationDTO();
        authDto.setIdentifier("admin@opensilex.org");
        authDto.setPassword("admin");

        final Response callResult = target("/security/authenticate")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(authDto, MediaType.APPLICATION_JSON_TYPE));

        JsonNode node = callResult.readEntity(JsonNode.class);

        // need to convert according a TypeReference, because the expected SingleObjectResponse is a generic object
        SingleObjectResponse<TokenGetDTO> res = mapper.convertValue(node, new TypeReference< SingleObjectResponse<TokenGetDTO> >() {
        });

        assertEquals(Response.Status.OK.getStatusCode(), callResult.getStatus());
        assertEquals(Response.Status.OK, res.getStatus());
        return res.getResult();
    }


    /**
     *
     * @param target
     * @param entity
     * @return
     */
    protected Response getJsonPostResponse(WebTarget target, Object entity) {
        return appendToken(target).post(Entity.entity(entity, MediaType.APPLICATION_JSON_TYPE));
    }

    /**
     *
     * @param target
     * @param entity
     * @return
     */
    protected Response getJsonPutResponse(WebTarget target, Object entity) {
        return appendToken(target).put(Entity.entity(entity, MediaType.APPLICATION_JSON_TYPE));
    }

    /**
     *
     * @param target
     * @param uri
     * @return
     */
    protected Response getJsonGetByUriResponse(WebTarget target, String uri) {
        return appendToken(target.resolveTemplate("uri", uri)).get();
    }

    /**
     *
     * @param target
     * @param uri
     * @return
     */
    protected Response getDeleteByUriResponse(WebTarget target, String uri) {
        return appendToken(target.resolveTemplate("uri", uri)).delete();
    }

    /**
     *
     * @param target
     * @param page
     * @param pageSize
     * @param params
     * @return
     */
    protected WebTarget appendSearchParams(WebTarget target, int page, int pageSize, Map<String, Object> params) {
        return appendSearchParams(target, page, pageSize, Collections.emptyList(), params);
    }

    /**
     *
     * @param target
     * @param page
     * @param pageSize
     * @param orderByList
     * @param params
     * @return
     */
    protected WebTarget appendSearchParams(WebTarget target, int page, int pageSize, List<OrderBy> orderByList, Map<String, Object> params) {

        target.queryParam("page", page)
                .queryParam("pageSize", pageSize)
                .queryParam("orderBy", orderByList);

        for (Map.Entry<String, Object> entry : params.entrySet()) {
            target = target.queryParam(entry.getKey(), entry.getValue());
        }
        return target;
    }

    /**
     *
     * @param target
     * @return
     */
    protected Invocation.Builder appendToken(WebTarget target) {
        TokenGetDTO token = getToken();
        return target.request(MediaType.APPLICATION_JSON_TYPE)
                .header(ApiProtected.HEADER_NAME, ApiProtected.TOKEN_PARAMETER_PREFIX + token.getToken());
    }

    /**
     *
     * @param response
     * @return
     * @throws URISyntaxException
     */
    protected URI extractUriFromResponse(final Response response) throws URISyntaxException {
        JsonNode node = response.readEntity(JsonNode.class);
        ObjectUriResponse postResponse = mapper.convertValue(node, new TypeReference<ObjectUriResponse>() {});
        return new URI(postResponse.getResult());
    }

    /**
     *
     * @param response
     * @return
     */
    protected List<URI> extractUriListFromResponse(final Response response) {
        JsonNode node = response.readEntity(JsonNode.class);
        PaginatedListResponse<URI> listResponse = mapper.convertValue(node, new TypeReference<PaginatedListResponse<URI> >() {});
        return listResponse.getResult();
    }

}

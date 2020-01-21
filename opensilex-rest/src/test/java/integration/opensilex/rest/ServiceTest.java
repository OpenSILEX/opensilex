package integration.opensilex.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.opensilex.rest.authentication.ApiProtected;
import org.opensilex.rest.security.api.AuthenticationDTO;
import org.opensilex.rest.security.api.TokenGetDTO;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.sparql.utils.OrderBy;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.assertEquals;

/**
 * @author Renaud COLIN
 */
public abstract class ServiceTest extends RestApplicationTest {

    protected static ObjectMapper mapper = new ObjectMapper();

    protected TokenGetDTO getToken() {

        AuthenticationDTO authDto = new AuthenticationDTO();
        authDto.setIdentifier("admin@opensilex.org");
        authDto.setPassword("admin");

        final Response callResult = target("/security/authenticate")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(authDto, MediaType.APPLICATION_JSON_TYPE));

        JsonNode node = callResult.readEntity(JsonNode.class);

        // need to convert according a TypeReference, because the expected SingleObjectResponse is a generic object
        // here we specify that we are expecting a SingleObjectResponse<TokenGetDTO>
        SingleObjectResponse<TokenGetDTO> res = mapper.convertValue(node, new TypeReference<SingleObjectResponse<TokenGetDTO>>() {
        });

        assertEquals(callResult.getStatus(), Response.Status.OK.getStatusCode());
        assertEquals(res.getStatus(), Response.Status.OK);
        return res.getResult();
    }

    protected Response getJsonPostResponse(WebTarget target, Object entity) {
        return appendToken(target).post(Entity.entity(entity, MediaType.APPLICATION_JSON_TYPE));
    }

    protected Response getJsonPutResponse(WebTarget target, Object entity) {
        return appendToken(target).put(Entity.entity(entity, MediaType.APPLICATION_JSON_TYPE));
    }

    protected Response getJsonGetByUriResponse(WebTarget target, String uri) {
        return appendToken(target.resolveTemplate("uri", uri)).get();
    }

    protected Response getDeleteByUriResponse(WebTarget target, String uri){
        return appendToken(target.resolveTemplate("uri", uri)).delete();
    }

    protected WebTarget appendSearchParams(WebTarget target, int page, int pageSize, Map<String, Object> params) {
        return appendSearchParams(target, page, pageSize, Collections.emptyList(),params);
    }

    protected WebTarget appendSearchParams(WebTarget target, int page, int pageSize, List<OrderBy> orderByList, Map<String, Object> params) {

        target.queryParam("page", page)
                .queryParam("pageSize", pageSize)
                .queryParam("orderBy", orderByList);

        for(Map.Entry<String,Object> entry : params.entrySet()){
            target = target.queryParam(entry.getKey(),entry.getValue());
        }
        return target;
    }

    protected Invocation.Builder appendToken(WebTarget target) {
        TokenGetDTO token = getToken();
        return target.request(MediaType.APPLICATION_JSON_TYPE)
                .header(ApiProtected.HEADER_NAME, ApiProtected.TOKEN_PARAMETER_PREFIX + token.getToken());
    }

    protected URI extractUriFromResponse(final Response response) throws URISyntaxException {
        JsonNode node = response.readEntity(JsonNode.class);
        ObjectUriResponse postResponse = mapper.convertValue(node, new TypeReference<>() {
        });
        return new URI(postResponse.getResult());
    }

    protected List<URI> extractUriListFromResponse(final Response response){
        JsonNode node = response.readEntity(JsonNode.class);
        PaginatedListResponse<URI> listResponse = mapper.convertValue(node, new TypeReference<>() {
        });
        return listResponse.getResult();
    }

}

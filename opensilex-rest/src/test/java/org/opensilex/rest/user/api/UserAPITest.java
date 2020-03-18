package org.opensilex.rest.user.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.opensilex.integration.test.AbstractIntegrationTest;
import org.junit.Test;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import javax.ws.rs.client.WebTarget;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import org.opensilex.OpenSilex;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;

public class UserAPITest extends AbstractIntegrationTest {

    /*
     * Uncomment this method to enable test debug logging
     */
//    protected boolean isDebug() {
//        return true;
//    }
    
    protected String path = "/user";
    protected String createPath = path + "/create";
    protected String updatePath = path + "/update";
    protected String getPath = path + "/get/{uri}";
    protected String deletePath = path + "/delete/{uri}";
    protected String searchPath = path + "/search";
    protected String urisListPath = path + "/get-list-by-uris";

    private int userCount = 0;
    protected UserCreationDTO getUser1CreationDTO() {
        int count = userCount++;
        
        UserCreationDTO dto = new UserCreationDTO();
        dto.setAdmin(true);
        dto.setEmail("user" + count + "@opensilex.org");
        dto.setFirstName("user" + count + "");
        dto.setLastName("user" + count + " last name");
        dto.setPassword("1234");
        dto.setLang(OpenSilex.DEFAULT_LANGUAGE);
        return dto;
    }

    protected UserCreationDTO getUser2CreationDTO() {
        int count = userCount++;
        
        UserCreationDTO dto = new UserCreationDTO();
        dto.setAdmin(false);
        dto.setEmail("user" + count + "@opensilex.org");
        dto.setFirstName("user" + count + "");
        dto.setLastName("user" + count + " last name");
        dto.setPassword("6789");
        dto.setLang(OpenSilex.DEFAULT_LANGUAGE);
        return dto;
    }

    @Test
    public void testCreate() throws Exception {
        Response postResult = getJsonPostResponse(target(createPath), getUser1CreationDTO());
        assertEquals(Response.Status.CREATED.getStatusCode(), postResult.getStatus());

        // ensure that the result is a well formed URI, else throw exception
        URI createdUri = extractUriFromResponse(postResult);
        Response getResult = getJsonGetByUriResponse(target(getPath), createdUri.toString());
        assertEquals(Response.Status.OK.getStatusCode(), getResult.getStatus());

        postResult = getJsonPostResponse(target(createPath), getUser2CreationDTO());
        assertEquals(Response.Status.CREATED.getStatusCode(), postResult.getStatus());

        // ensure that the result is a well formed URI, else throw exception
        createdUri = extractUriFromResponse(postResult);
        getResult = getJsonGetByUriResponse(target(getPath), createdUri.toString());
        assertEquals(Response.Status.OK.getStatusCode(), getResult.getStatus());
    }

    @Test
    public void testUpdate() throws Exception {
        // create the user
        Response postResult = getJsonPostResponse(target(createPath), getUser1CreationDTO());

        // update the xp
        URI uri = extractUriFromResponse(postResult);
        UserUpdateDTO dto = new UserUpdateDTO();
        dto.setUri(uri);
        dto.setEmail("a@b.com");
        dto.setFirstName("a");
        dto.setLastName("b");
        dto.setAdmin(false);
        dto.setLang("fr-FR");

        final Response putResult = getJsonPutResponse(target(updatePath), dto);
        assertEquals(Response.Status.OK.getStatusCode(), putResult.getStatus());

        // retrieve the new xp and compare to the expected xp
        final Response getResult = getJsonGetByUriResponse(target(getPath), dto.getUri().toString());

        // try to deserialize object
        JsonNode node = getResult.readEntity(JsonNode.class);
        ObjectMapper mapper = new ObjectMapper();
        SingleObjectResponse<UserGetDTO> getResponse = mapper.convertValue(node, new TypeReference<SingleObjectResponse<UserGetDTO>>() {
        });
        UserGetDTO dtoFromApi = getResponse.getResult();

        // check that the object has been updated
        compareUsersDTO(dto, dtoFromApi);
    }

    @Test
    public void testDelete() throws Exception {
        // create object and check if URI exists
        Response postResponse = getJsonPostResponse(target(createPath), getUser1CreationDTO());
        assertEquals(Response.Status.CREATED.getStatusCode(), postResponse.getStatus());
        String uri = extractUriFromResponse(postResponse).toString();

        // delete object and if URI no longer exists
        Response delResult = getDeleteByUriResponse(target(deletePath), uri);
        assertEquals(Response.Status.OK.getStatusCode(), delResult.getStatus());

        Response getResult = getJsonGetByUriResponse(target(getPath), uri);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), getResult.getStatus());
    }

    @Test
    public void testGetByUriFail() throws Exception {
        final Response postResponse = getJsonPostResponse(target(createPath), getUser1CreationDTO());
        String uri = extractUriFromResponse(postResponse).toString();

        // call the service with a non existing pseudo random URI
        final Response getResult = getJsonGetByUriResponse(target(getPath), uri + "7FG4FG89FG4GH4GH57");
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), getResult.getStatus());
    }

    @Test
    public void testSearch() throws Exception {
        Response postResult = getJsonPostResponse(target(createPath), getUser1CreationDTO());
        assertEquals(Response.Status.CREATED.getStatusCode(), postResult.getStatus());

        postResult = getJsonPostResponse(target(createPath), getUser2CreationDTO());
        assertEquals(Response.Status.CREATED.getStatusCode(), postResult.getStatus());

        Map<String, Object> params = new HashMap<String, Object>() {
            {
                put("pattern", "user.*");
            }
        };

        WebTarget target = appendSearchParams(target(searchPath), 0, 50, params);
        Response getSearchResult = appendToken(target).get();
        assertEquals(Response.Status.OK.getStatusCode(), getSearchResult.getStatus());

        JsonNode node = getSearchResult.readEntity(JsonNode.class);
        ObjectMapper mapper = new ObjectMapper();
        PaginatedListResponse<UserGetDTO> listResponse = mapper.convertValue(node, new TypeReference<PaginatedListResponse<UserGetDTO>>() {
        });
        List<UserGetDTO> users = listResponse.getResult();

        assertFalse(users.isEmpty());
        assertEquals(2, users.size());

        params.put("pattern", "user1");
        target = appendSearchParams(target(searchPath), 0, 50, params);
        getSearchResult = appendToken(target).get();
        assertEquals(Response.Status.OK.getStatusCode(), getSearchResult.getStatus());

        node = getSearchResult.readEntity(JsonNode.class);
        listResponse = mapper.convertValue(node, new TypeReference<PaginatedListResponse<UserGetDTO>>() {
        });
        users = listResponse.getResult();
        assertEquals(1, users.size());
    }

    @Test
    public void testGetByURIs() throws Exception {
        Response postResult = getJsonPostResponse(target(createPath), getUser1CreationDTO());
        assertEquals(Response.Status.CREATED.getStatusCode(), postResult.getStatus());
        URI user1URI = extractUriFromResponse(postResult);

        postResult = getJsonPostResponse(target(createPath), getUser2CreationDTO());
        assertEquals(Response.Status.CREATED.getStatusCode(), postResult.getStatus());
        URI user2URI = extractUriFromResponse(postResult);

        Map<String, Object> params = new HashMap<String, Object>() {
            {
                put("uris", Arrays.asList(user1URI, user2URI));
            }
        };

        WebTarget target = appendQueryParams(target(urisListPath), params);
        Response getResult = appendToken(target).get();
        assertEquals(Response.Status.OK.getStatusCode(), getResult.getStatus());

        JsonNode node = getResult.readEntity(JsonNode.class);
        ObjectMapper mapper = new ObjectMapper();
        PaginatedListResponse<UserGetDTO> listResponse = mapper.convertValue(node, new TypeReference<PaginatedListResponse<UserGetDTO>>() {
        });
        List<UserGetDTO> users = listResponse.getResult();

        assertFalse(users.isEmpty());
        assertEquals(2, users.size());
    }

    private void compareUsersDTO(UserGetDTO expectedUserDTO, UserGetDTO actualUserDTO) {
        assertEquals(expectedUserDTO.getUri(), actualUserDTO.getUri());
        assertEquals(expectedUserDTO.getEmail(), actualUserDTO.getEmail());
        assertEquals(expectedUserDTO.getFirstName(), actualUserDTO.getFirstName());
        assertEquals(expectedUserDTO.getLastName(), actualUserDTO.getLastName());
        assertEquals(expectedUserDTO.isAdmin(), actualUserDTO.isAdmin());
        assertEquals(expectedUserDTO.getLang(), actualUserDTO.getLang());
    }
}

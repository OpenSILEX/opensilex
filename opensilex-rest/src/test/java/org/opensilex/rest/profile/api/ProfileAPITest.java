package org.opensilex.rest.profile.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import static io.swagger.util.Yaml.mapper;
import org.opensilex.integration.test.AbstractIntegrationTest;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import javax.ws.rs.client.WebTarget;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import org.junit.Assert;
import org.junit.Test;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;

public class ProfileAPITest extends AbstractIntegrationTest {

    /*
     * Uncomment this method to enable test debug logging
     */
//    protected boolean isDebug() {
//        return true;
//    }
    
    protected String path = "/profile";
    protected String createPath = path + "/create";
    protected String updatePath = path + "/update";
    protected String getPath = path + "/get/{uri}";
    protected String deletePath = path + "/delete/{uri}";
    protected String searchPath = path + "/search";
    protected String getAllPath = path + "/get-all";

    protected ProfileCreationDTO getProfilCreationDTO() {

        ProfileCreationDTO dto = new ProfileCreationDTO();
        dto.setName("profile 1");
        dto.setCredentials(new ArrayList<String>() {
            {
                add("cred1");
                add("cred2");
                add("cred3");
            }
        });
        return dto;
    }

    @Test
    public void testCreate() throws Exception {

        Response postResult = getJsonPostResponse(target(createPath), getProfilCreationDTO());
        assertEquals(Response.Status.CREATED.getStatusCode(), postResult.getStatus());

        // ensure that the result is a well formed URI, else throw exception
        URI createdUri = extractUriFromResponse(postResult);
        Response getResult = getJsonGetByUriResponse(target(getPath), createdUri.toString());
        assertEquals(Response.Status.OK.getStatusCode(), getResult.getStatus());
    }

    @Test
    public void testUpdate() throws Exception {

        // create the user
        Response postResult = getJsonPostResponse(target(createPath), getProfilCreationDTO());

        // update the xp
        URI uri = extractUriFromResponse(postResult);
        ProfileUpdateDTO dto = new ProfileUpdateDTO();
        dto.setUri(uri);
        dto.setName("new name");
        dto.setCredentials(new ArrayList<String>() {
            {
                add("new-cred1");
                add("new-cred2");
            }
        });

        final Response putResult = getJsonPutResponse(target(updatePath), dto);
        assertEquals(Response.Status.OK.getStatusCode(), putResult.getStatus());

        // retrieve the new xp and compare to the expected xp
        final Response getResult = getJsonGetByUriResponse(target(getPath), dto.getUri().toString());

        // try to deserialize object
        JsonNode node = getResult.readEntity(JsonNode.class);
        ObjectMapper mapper = new ObjectMapper();
        SingleObjectResponse<ProfileGetDTO> getResponse = mapper.convertValue(node, new TypeReference<SingleObjectResponse<ProfileGetDTO>>() {
        });
        ProfileGetDTO dtoFromApi = getResponse.getResult();

        // check that the object has been updated
        compareProfilesDTO(dto, dtoFromApi);
    }

    @Test
    public void testDelete() throws Exception {

        // create object and check if URI exists
        Response postResponse = getJsonPostResponse(target(createPath), getProfilCreationDTO());
        assertEquals(Response.Status.CREATED.getStatusCode(), postResponse.getStatus());
        String uri = extractUriFromResponse(postResponse).toString();

        // delete object and if URI no longer exists
        Response delResult = getDeleteByUriResponse(target(deletePath), uri);
        assertEquals(Response.Status.OK.getStatusCode(), delResult.getStatus());

        Response getResult = getJsonGetByUriResponse(target(getPath), uri);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), getResult.getStatus());
    }

    @Test
    public void testSearch() throws Exception {

        Response postResult = getJsonPostResponse(target(createPath), getProfilCreationDTO());
        assertEquals(Response.Status.CREATED.getStatusCode(), postResult.getStatus());

        ProfileCreationDTO profile2 = new ProfileCreationDTO();
        profile2.setName("profile 2");
        profile2.setCredentials(new ArrayList<String>() {
            {
                add("cred2");
                add("cred3");
                add("cred4");
            }
        });

        postResult = getJsonPostResponse(target(createPath), profile2);
        assertEquals(Response.Status.CREATED.getStatusCode(), postResult.getStatus());

        Map<String, Object> params = new HashMap<String, Object>() {
            {
                put("pattern", "profile.*");
            }
        };

        WebTarget target = appendSearchParams(target(searchPath), 0, 50, params);
        Response getSearchResult = appendToken(target).get();
        assertEquals(Response.Status.OK.getStatusCode(), getSearchResult.getStatus());

        JsonNode node = getSearchResult.readEntity(JsonNode.class);
        ObjectMapper mapper = new ObjectMapper();
        PaginatedListResponse<ProfileGetDTO> listResponse = mapper.convertValue(node, new TypeReference<PaginatedListResponse<ProfileGetDTO>>() {
        });
        List<ProfileGetDTO> users = listResponse.getResult();

        assertFalse(users.isEmpty());
        assertEquals(2, users.size());

        params.put("pattern", "profile 2");
        target = appendSearchParams(target(searchPath), 0, 50, params);
        getSearchResult = appendToken(target).get();
        assertEquals(Response.Status.OK.getStatusCode(), getSearchResult.getStatus());

        node = getSearchResult.readEntity(JsonNode.class);
        listResponse = mapper.convertValue(node, new TypeReference<PaginatedListResponse<ProfileGetDTO>>() {
        });
        users = listResponse.getResult();
        assertEquals(1, users.size());
    }

    @Test
    public void testGetAll() throws Exception {

        Response postResult = getJsonPostResponse(target(createPath), getProfilCreationDTO());
        assertEquals(Response.Status.CREATED.getStatusCode(), postResult.getStatus());

        ProfileCreationDTO profile2 = new ProfileCreationDTO();
        profile2.setName("profile 2");
        profile2.setCredentials(new ArrayList<String>() {
            {
                add("cred2");
                add("cred3");
                add("cred4");
            }
        });

        postResult = getJsonPostResponse(target(createPath), profile2);
        assertEquals(Response.Status.CREATED.getStatusCode(), postResult.getStatus());

        Response getAllResult = appendToken(target(getAllPath)).get();
        assertEquals(Response.Status.OK.getStatusCode(), getAllResult.getStatus());

        JsonNode node = getAllResult.readEntity(JsonNode.class);
        ObjectMapper mapper = new ObjectMapper();
        PaginatedListResponse<ProfileGetDTO> listResponse = mapper.convertValue(node, new TypeReference<PaginatedListResponse<ProfileGetDTO>>() {
        });
        List<ProfileGetDTO> users = listResponse.getResult();

        assertFalse(users.isEmpty());
        assertEquals(2, users.size());
    }

    private void compareProfilesDTO(ProfileGetDTO expectedProfileDTO, ProfileGetDTO actualProfileDTO) {
        assertEquals(expectedProfileDTO.getUri(), actualProfileDTO.getUri());
        assertEquals(expectedProfileDTO.getName(), actualProfileDTO.getName());
        expectedProfileDTO.getCredentials().sort(String::compareTo);
        String[] expectedCredentials = expectedProfileDTO.getCredentials().toArray(new String[expectedProfileDTO.getCredentials().size()]);

        actualProfileDTO.getCredentials().sort(String::compareTo);
        String[] actualCredentials = actualProfileDTO.getCredentials().toArray(new String[actualProfileDTO.getCredentials().size()]);
        Assert.assertArrayEquals(expectedCredentials, actualCredentials);
    }

    @Override
    protected List<String> getGraphsToCleanNames() {
        return Collections.singletonList("profiles");
    }
}

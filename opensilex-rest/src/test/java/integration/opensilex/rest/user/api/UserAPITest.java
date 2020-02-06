package integration.opensilex.rest.user.api;

import integration.opensilex.AbstractIntegrationTest;
import org.junit.Test;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import static junit.framework.TestCase.assertEquals;
import org.opensilex.rest.user.api.UserCreationDTO;

public class UserAPITest extends AbstractIntegrationTest {

    protected String path = "/user";
    protected String createPath = path + "/create";
    protected String getPath = path + "/get/{uri}";
    protected String uriPath = path + "/{uri}";
    protected String searchPath = path + "/search";

    protected UserCreationDTO getUser1CreationDTO() {

        UserCreationDTO dto = new UserCreationDTO();
        dto.setAdmin(true);
        dto.setEmail("user1@opensilex.org");
        dto.setFirstName("user1");
        dto.setLastName("user1 last name");
        dto.setPassword("1234");
        return dto;
    }
    
    protected UserCreationDTO getUser2CreationDTO() {
        UserCreationDTO dto = new UserCreationDTO();
        dto.setAdmin(false);
        dto.setEmail("user2@opensilex.org");
        dto.setFirstName("user2");
        dto.setLastName("user2 last name");
        dto.setPassword("6789");
        return dto;
    }

    @Test
    public void testCreate() throws URISyntaxException {

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
//
//    @Test
//    public void testUpdate() throws URISyntaxException {
//
//        // create the xp
//        ExperimentCreationDTO xpDto = getCreationDTO();
//        final Response postResult = getJsonPostResponse(target(path), xpDto);
//
//        // update the xp
//        xpDto.setUri(extractUriFromResponse(postResult));
//        xpDto.setLabel("new alias");
//        xpDto.setEndDate(LocalDate.now().toString());
//
//        final Response putResult = getJsonPutResponse(target(path), xpDto);
//        assertEquals(Response.Status.OK.getStatusCode(), putResult.getStatus());
//
//        // retrieve the new xp and compare to the expected xp
//        final Response getResult = getJsonGetByUriResponse(target(uriPath), xpDto.getUri().toString());
//
//        // try to deserialize object
//        JsonNode node = getResult.readEntity(JsonNode.class);
//        SingleObjectResponse<ExperimentGetDTO> getResponse = mapper.convertValue(node, new TypeReference<SingleObjectResponse<ExperimentGetDTO>>() {
//        });
//        ExperimentGetDTO dtoFromApi = getResponse.getResult();
//
//        // check that the object has been updated
//        assertEquals(xpDto.getLabel(), dtoFromApi.getLabel());
//        assertEquals(xpDto.getEndDate(), dtoFromApi.getEndDate());
//    }

//    @Test
//    public void testDelete() throws URISyntaxException {
//
//        // create object and check if URI exists
//        Response postResponse = getJsonPostResponse(target(path), getCreationDTO());
//        assertEquals(Response.Status.CREATED.getStatusCode(), postResponse.getStatus());
//        String uri = extractUriFromResponse(postResponse).toString();
//
//        Response getResult = getJsonGetByUriResponse(target(uriPath), uri);
//        assertEquals(Response.Status.OK.getStatusCode(), getResult.getStatus());
//
//        // delete object and if URI no longer exists
//        Response delResult = getDeleteByUriResponse(target(uriPath), uri);
//        assertEquals(Response.Status.OK.getStatusCode(), delResult.getStatus());
//
//        getResult = getJsonGetByUriResponse(target(uriPath), uri);
//        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), getResult.getStatus());
//    }
//
//    @Test
//    public void testGetByUri() throws URISyntaxException {
//
//        final Response postResult = getJsonPostResponse(target(path), getCreationDTO());
//        URI uri = extractUriFromResponse(postResult);
//
//        final Response getResult = getJsonGetByUriResponse(target(uriPath), uri.toString());
//        assertEquals(Response.Status.OK.getStatusCode(), getResult.getStatus());
//
//        // try to deserialize object
//        JsonNode node = getResult.readEntity(JsonNode.class);
//        SingleObjectResponse<ExperimentGetDTO> getResponse = mapper.convertValue(node, new TypeReference<SingleObjectResponse<ExperimentGetDTO>>() {
//        });
//        ExperimentGetDTO xpGetDto = getResponse.getResult();
//        assertNotNull(xpGetDto);
//    }
//
//    @Test
//    public void testGetByUriFail() {
//
//        final Response postResult = getJsonPostResponse(target(path), getCreationDTO());
//        JsonNode node = postResult.readEntity(JsonNode.class);
//        ObjectUriResponse postResponse = mapper.convertValue(node, ObjectUriResponse.class);
//        String uri = postResponse.getResult();
//
//        // call the service with a non existing pseudo random URI
//        final Response getResult = getJsonGetByUriResponse(target(uriPath), uri + "7FG4FG89FG4GH4GH57");
//        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), getResult.getStatus());
//    }
//
//    @Test
//    public void testSearch() throws URISyntaxException {
//
//        ExperimentCreationDTO creationDTO = getCreationDTO();
//        final Response postResult = getJsonPostResponse(target(path), creationDTO);
//        URI uri = extractUriFromResponse(postResult);
//
//        Map<String, Object> params = new HashMap<String, Object>() {
//            {
//                put("campaign", creationDTO.getCampaign());
//                put("startDate", creationDTO.getStartDate());
//                put("label", creationDTO.getLabel());
//                put("uri", uri);
//            }
//        };
//
//        WebTarget target = appendSearchParams(target(searchPath), 0, 50, params);
//        final Response getResult = appendToken(target).get();
//        assertEquals(Response.Status.OK.getStatusCode(), getResult.getStatus());
//
//        JsonNode node = getResult.readEntity(JsonNode.class);
//        PaginatedListResponse<ExperimentGetDTO> xpListResponse = mapper.convertValue(node, PaginatedListResponse.class);
//        List<ExperimentGetDTO> xps = xpListResponse.getResult();
//
//        assertFalse(xps.isEmpty());
//    }
//
    @Override
    protected List<String> getGraphsToCleanNames() {
        return Collections.singletonList("users");
    }
}

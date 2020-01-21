package org.opensilex.core.experiment.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import integration.opensilex.rest.ServiceTest;
import org.junit.Test;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotSame;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class ExperimentAPITest extends ServiceTest {

    protected String path = "/core/experiment";
    protected String uriPath = path+"/{uri}";
    protected String searchPath = path+"/search";

    protected ExperimentCreationDTO getCreationDTO() {

        ExperimentCreationDTO xpDto = new ExperimentCreationDTO();
        xpDto.setLabel("xp");

        LocalDate currentDate = LocalDate.now();
        xpDto.setStartDate(currentDate.minusDays(3).toString());
        xpDto.setEndDate(currentDate.plusDays(3).toString());
        xpDto.setCampaign(currentDate.getYear());
        return xpDto;
    }

    @Test
    public void testCreate() throws URISyntaxException {

        final Response postResult = getJsonPostResponse(target(path), getCreationDTO());
        assertEquals(Status.CREATED.getStatusCode(), postResult.getStatus());

        // ensure that the result is a well formed URI, else throw exception
        URI createdUri = extractUriFromResponse(postResult);
        final Response getResult = getJsonGetByUriResponse(target(uriPath), createdUri.toString());
        assertEquals(Status.OK.getStatusCode(), getResult.getStatus());
    }

    @Test
    public void testCreateAll() throws URISyntaxException {

        List<ExperimentCreationDTO> xpDtos = Arrays.asList( getCreationDTO(),getCreationDTO());
        String createAllPath = path+"/experiments";

        final Response postResult = getJsonPostResponse(target(createAllPath), xpDtos);
        assertEquals(Status.CREATED.getStatusCode(), postResult.getStatus());

        List<URI> xpUris = extractUriListFromResponse(postResult);
        for(URI xpUri : xpUris){
            final Response getResult = getJsonGetByUriResponse(target(uriPath), xpUri.toString());
            assertEquals(Status.OK.getStatusCode(), getResult.getStatus());
        }
    }

    @Test
    public void testCreateFailWithBadDateFormats() {

        ExperimentCreationDTO creationDTO = getCreationDTO();
        creationDTO.setStartDate("07-08-2015");

        final Response postResult = getJsonPostResponse(target(path), getCreationDTO());
        assertNotSame(Status.OK.getStatusCode(), postResult.getStatus());
    }

    @Test
    public void testUpdate() throws URISyntaxException {

        // create the xp
        ExperimentCreationDTO xpDto = getCreationDTO();
        final Response postResult = getJsonPostResponse(target(path), xpDto);

        // update the xp
        xpDto.setUri(extractUriFromResponse(postResult));
        xpDto.setLabel("new alias");
        xpDto.setEndDate(LocalDate.now().toString());

        final Response putResult = getJsonPutResponse(target(path), xpDto);
        assertEquals(Status.OK.getStatusCode(), putResult.getStatus());

        // retrieve the new xp and compare to the expected xp
        final Response getResult = getJsonGetByUriResponse(target(uriPath), xpDto.getUri().toString());

        // try to deserialize object
        JsonNode node = getResult.readEntity(JsonNode.class);
        SingleObjectResponse<ExperimentGetDTO> getResponse = mapper.convertValue(node, new TypeReference<>() {});
        ExperimentGetDTO dtoFromApi = getResponse.getResult();

        // check that the object has been updated
        assertEquals(xpDto.getLabel(), dtoFromApi.getLabel());
        assertEquals(xpDto.getEndDate(), dtoFromApi.getEndDate().toString());
    }

    @Test
    public void testDelete() throws URISyntaxException {

        // create object and check if URI exists
        Response postResponse = getJsonPostResponse(target(path), getCreationDTO());
        assertEquals(Status.CREATED.getStatusCode(), postResponse.getStatus());
        String uri = extractUriFromResponse(postResponse).toString();

        Response getResult = getJsonGetByUriResponse(target(uriPath), uri);
        assertEquals(Status.OK.getStatusCode(), getResult.getStatus());

        // delete object and if URI no longer exists
        Response delResult = getDeleteByUriResponse(target(uriPath), uri);
        assertEquals(Status.OK.getStatusCode(), delResult.getStatus());

        getResult = getJsonGetByUriResponse(target(uriPath), uri);
        assertEquals(Status.NOT_FOUND.getStatusCode(), getResult.getStatus());
    }

    @Test
    public void testGetByUri() throws URISyntaxException {

        final Response postResult = getJsonPostResponse(target(path), getCreationDTO());
        URI uri = extractUriFromResponse(postResult);

        final Response getResult = getJsonGetByUriResponse(target(uriPath),uri.toString());
        assertEquals(Status.OK.getStatusCode(), getResult.getStatus());

        // try to deserialize object
        JsonNode node = getResult.readEntity(JsonNode.class);
        SingleObjectResponse<ExperimentGetDTO> getResponse = mapper.convertValue(node, new TypeReference<>() {});
        ExperimentGetDTO xpGetDto = getResponse.getResult();
        assertNotNull(xpGetDto);
    }


    @Test
    public void testGetByUriFail() {

        final Response postResult = getJsonPostResponse(target(path), getCreationDTO());
        JsonNode node = postResult.readEntity(JsonNode.class);
        ObjectUriResponse postResponse = mapper.convertValue(node, new TypeReference<>() {});
        String uri = postResponse.getResult();

        // call the service with a non existing pseudo random URI
        final Response getResult = getJsonGetByUriResponse(target(uriPath),uri + "7FG4FG89FG4GH4GH57");
        assertEquals(Status.NOT_FOUND.getStatusCode(), getResult.getStatus());
    }

    @Test
    public void testSearch() throws URISyntaxException {

        ExperimentCreationDTO creationDTO = getCreationDTO();
        final Response postResult = getJsonPostResponse(target(path), creationDTO);
        URI uri = extractUriFromResponse(postResult);

        Map<String, Object> params  = new HashMap<>() {{
            put("campaign", creationDTO.getCampaign());
            put("startDate", creationDTO.getStartDate());
            put("label",creationDTO.getLabel());
            put("uri",uri);
        }};

        WebTarget target = appendSearchParams(target(searchPath), 0, 50,params);
        final Response getResult = appendToken(target).get();
        assertEquals(Status.OK.getStatusCode(), getResult.getStatus());

        JsonNode node = getResult.readEntity(JsonNode.class);
        PaginatedListResponse<ExperimentGetDTO> xpListResponse = mapper.convertValue(node, new TypeReference<>() {});
        List<ExperimentGetDTO> xps = xpListResponse.getResult();

        assertFalse(xps.isEmpty());
    }
}

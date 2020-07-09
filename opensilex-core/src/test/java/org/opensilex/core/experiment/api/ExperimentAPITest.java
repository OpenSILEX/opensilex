//******************************************************************************
//                          ExperimentAPITest.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.experiment.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;
import org.opensilex.server.response.ObjectUriResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.net.URI;
import java.time.LocalDate;
import java.util.*;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.integration.test.security.AbstractSecurityIntegrationTest;
import org.opensilex.sparql.model.SPARQLResourceModel;

/**
 * @author Vincent MIGOT
 * @author Renaud COLIN
 */
public class ExperimentAPITest extends AbstractSecurityIntegrationTest {

    protected String path = "/core/experiment";

    protected String uriPath = path + "/get/{uri}";
    protected String searchPath = path + "/search";
    protected String createPath = path + "/create";
    protected String updatePath = path + "/update";
    protected String deletePath = path + "/delete/{uri}";

    protected ExperimentCreationDTO getCreationDTO() {

        ExperimentCreationDTO xpDto = new ExperimentCreationDTO();
        xpDto.setLabel("xp");

        LocalDate currentDate = LocalDate.now();
        xpDto.setStartDate(currentDate.minusDays(3));
        xpDto.setEndDate(currentDate.plusDays(3));
        xpDto.setCampaign(currentDate.getYear());
        xpDto.setObjective("Objective");
        return xpDto;
    }

    @Test
    public void testCreate() throws Exception {

        final Response postResult = getJsonPostResponse(target(createPath), getCreationDTO());
        assertEquals(Status.CREATED.getStatusCode(), postResult.getStatus());

        // ensure that the result is a well formed URI, else throw exception
        URI createdUri = extractUriFromResponse(postResult);
        final Response getResult = getJsonGetByUriResponse(target(uriPath), createdUri.toString());
        assertEquals(Status.OK.getStatusCode(), getResult.getStatus());
    }

    @Test
    public void testCreateAll() throws Exception {

        List<ExperimentCreationDTO> creationDTOS = Arrays.asList(getCreationDTO(), getCreationDTO());

        for (ExperimentCreationDTO creationDTO : creationDTOS) {
            final Response postResult = getJsonPostResponse(target(createPath), creationDTO);
            assertEquals(Status.CREATED.getStatusCode(), postResult.getStatus());

            URI uri = extractUriFromResponse(postResult);
            final Response getResult = getJsonGetByUriResponse(target(uriPath), uri.toString());
            assertEquals(Status.OK.getStatusCode(), getResult.getStatus());
        }

    }

    @Test
    public void testUpdate() throws Exception {

        // create the xp
        ExperimentCreationDTO xpDto = getCreationDTO();
        final Response postResult = getJsonPostResponse(target(createPath), xpDto);

        // update the xp
        xpDto.setUri(extractUriFromResponse(postResult));
        xpDto.setLabel("new alias");
        xpDto.setEndDate(LocalDate.now());

        final Response updateResult = getJsonPutResponse(target(updatePath), xpDto);
        assertEquals(Status.OK.getStatusCode(), updateResult.getStatus());

        // retrieve the new xp and compare to the expected xp
        final Response getResult = getJsonGetByUriResponse(target(uriPath), xpDto.getUri().toString());

        // try to deserialize object
        JsonNode node = getResult.readEntity(JsonNode.class);
        SingleObjectResponse<ExperimentGetDTO> getResponse = mapper.convertValue(node, new TypeReference<SingleObjectResponse<ExperimentGetDTO>>() {
        });
        ExperimentGetDTO dtoFromApi = getResponse.getResult();

        // check that the object has been updated
        assertEquals(xpDto.getLabel(), dtoFromApi.getLabel());
        assertEquals(xpDto.getEndDate(), dtoFromApi.getEndDate());
    }

    @Test
    public void testDelete() throws Exception {

        // create object and check if URI exists
        Response postResponse = getJsonPostResponse(target(createPath), getCreationDTO());
        String uri = extractUriFromResponse(postResponse).toString();

        // delete object and check if URI no longer exists
        Response delResult = getDeleteByUriResponse(target(deletePath), uri);
        assertEquals(Status.OK.getStatusCode(), delResult.getStatus());

        Response getResult = getJsonGetByUriResponse(target(uriPath), uri);
        assertEquals(Status.NOT_FOUND.getStatusCode(), getResult.getStatus());
    }

    @Test
    public void testGetByUri() throws Exception {

        final Response postResult = getJsonPostResponse(target(createPath), getCreationDTO());
        URI uri = extractUriFromResponse(postResult);

        final Response getResult = getJsonGetByUriResponse(target(uriPath), uri.toString());
        assertEquals(Status.OK.getStatusCode(), getResult.getStatus());

        // try to deserialize object
        JsonNode node = getResult.readEntity(JsonNode.class);
        SingleObjectResponse<ExperimentGetDTO> getResponse = mapper.convertValue(node, new TypeReference<SingleObjectResponse<ExperimentGetDTO>>() {
        });
        ExperimentGetDTO xpGetDto = getResponse.getResult();
        assertNotNull(xpGetDto);
    }

    @Test
    public void testGetByUriFail() throws Exception {

        final Response postResult = getJsonPostResponse(target(createPath), getCreationDTO());
        JsonNode node = postResult.readEntity(JsonNode.class);
        ObjectUriResponse postResponse = mapper.convertValue(node, ObjectUriResponse.class);
        String uri = postResponse.getResult();

        // call the service with a non existing pseudo random URI
        final Response getResult = getJsonGetByUriResponse(target(uriPath), uri + "7FG4FG89FG4GH4GH57");
        assertEquals(Status.NOT_FOUND.getStatusCode(), getResult.getStatus());
    }

    @Test
    public void testSearch() throws Exception {

        ExperimentCreationDTO creationDTO = getCreationDTO();
        final Response postResult = getJsonPostResponse(target(createPath), creationDTO);
        URI uri = extractUriFromResponse(postResult);

        Map<String, Object> params = new HashMap<String, Object>() {
            {
                put("campaign", creationDTO.getCampaign());
                put("startDate", creationDTO.getStartDate());
                put("label", creationDTO.getLabel());
                put("uri", uri);
            }
        };

        WebTarget searchTarget = appendSearchParams(target(searchPath), 0, 50, params);
        final Response getResult = appendToken(searchTarget).get();
        assertEquals(Status.OK.getStatusCode(), getResult.getStatus());

        JsonNode node = getResult.readEntity(JsonNode.class);
        PaginatedListResponse<ExperimentGetDTO> xpListResponse = mapper.convertValue(node, new TypeReference<PaginatedListResponse<ExperimentGetDTO>>() {
        });
        List<ExperimentGetDTO> xps = xpListResponse.getResult();

        assertFalse(xps.isEmpty());
    }

    @Override
    protected List<Class<? extends SPARQLResourceModel>> getModelsToClean() {
        return Collections.singletonList(ExperimentModel.class);
    }
}

package org.opensilex.core.variable.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.variable.api.dimension.DimensionAPI;
import org.opensilex.core.variable.api.dimension.DimensionCreationDTO;
import org.opensilex.core.variable.api.dimension.DimensionDetailsDTO;
import org.opensilex.core.variable.dal.DimensionModel;
import org.opensilex.integration.test.security.AbstractSecurityIntegrationTest;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.sparql.model.SPARQLResourceModel;

import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Collections;
import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DimensionApiTest extends AbstractSecurityIntegrationTest {

    public static final String path = DimensionAPI.PATH;

    public static final String getByUriPath = path + "/{uri}";
    public static final String createPath = path;
    public static final String updatePath = path;
    public static final String deletePath = path + "/{uri}";

    public static DimensionCreationDTO getCreationDto() {
        DimensionCreationDTO dto = new DimensionCreationDTO();
        dto.setName("blue");
        dto.setDescription("blue signal from rgb");
        dto.setDatatype(URI.create("http://www.w3.org/2001/XMLSchema#integer"));
        return dto;
    }

    @Test
    public void testCreateGetAndDelete() throws Exception {
        super.testCreateGetAndDelete(createPath,getByUriPath, deletePath, getCreationDto());
    }

    @Test
    public void testCreateFailWithNoRequiredFields() throws Exception {

        DimensionCreationDTO dtoWithNoName = new DimensionCreationDTO();
        dtoWithNoName.setDescription("only a comment, not a name");

        final Response postResult = getJsonPostResponse(target(createPath), dtoWithNoName);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), postResult.getStatus());
    }

    @Test
    public void testGetByUriWithUnknownUri() throws Exception {
        Response getResult = getJsonGetByUriResponse(target(getByUriPath), Oeso.Dimension+"/58165");
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), getResult.getStatus());
    }

    @Test
    public void testUpdate() throws Exception {

        DimensionCreationDTO dto = getCreationDto();
        final Response postResult = getJsonPostResponse(target(createPath), dto);

        dto.setUri(extractUriFromResponse(postResult));
        dto.setName("new alias");
        dto.setDescription("new comment");

        final Response updateResult = getJsonPutResponse(target(updatePath), dto);
        assertEquals(Response.Status.OK.getStatusCode(), updateResult.getStatus());

        // retrieve the new dimension and compare to the expected dimension
        final Response getResult = getJsonGetByUriResponse(target(getByUriPath), dto.getUri().toString());

        // try to deserialize object
        JsonNode node = getResult.readEntity(JsonNode.class);
        SingleObjectResponse<DimensionCreationDTO> getResponse = mapper.convertValue(node, new TypeReference<SingleObjectResponse<DimensionCreationDTO>>() {});
        DimensionCreationDTO dtoFromApi = getResponse.getResult();

        // check that the object has been updated
        assertEquals(dto.getName(), dtoFromApi.getName());
        assertEquals(dto.getDescription(), dtoFromApi.getDescription());
    }

    @Test
    public void testGetByUri() throws Exception {

        // Try to insert a Dimension, to fetch it and to get fields
        DimensionCreationDTO creationDTO = getCreationDto();
        Response postResult = getJsonPostResponse(target(createPath), creationDTO);
        URI uri = extractUriFromResponse(postResult);

        Response getResult = getJsonGetByUriResponse(target(getByUriPath), uri.toString());

        // try to deserialize object and check if the fields value are the same
        JsonNode node = getResult.readEntity(JsonNode.class);
        SingleObjectResponse<DimensionDetailsDTO> getResponse =  mapper.convertValue(node, new TypeReference<SingleObjectResponse<DimensionDetailsDTO>>() {});
        DimensionDetailsDTO dtoFromDb = getResponse.getResult();
        assertNotNull(dtoFromDb);
        assertEquals(creationDTO.getName(), dtoFromDb.getName());
        assertEquals(creationDTO.getDescription(), dtoFromDb.getDescription());

    }

    @Override
    protected List<Class<? extends SPARQLResourceModel>> getModelsToClean() {
        return Collections.singletonList(DimensionModel.class);
    }
}
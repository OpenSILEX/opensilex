//******************************************************************************
//                          ScientificObjectAPITest.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.scientificObject.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.client.model.geojson.Geometry;
import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Polygon;
import com.mongodb.client.model.geojson.Position;
import org.junit.Test;
import org.opensilex.core.experiment.api.ExperimentCreationDTO;
import org.opensilex.integration.test.security.AbstractSecurityIntegrationTest;
import org.opensilex.server.response.SingleObjectResponse;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.net.URI;
import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.opensilex.core.geospatial.dal.GeospatialDAO.geometryToGeoJson;

/**
 * @author Vincent MIGOT
 * @author Jean Philippe VERT
 */
public class ScientificObjectAPITest extends AbstractSecurityIntegrationTest {

    protected final String path = "/core/scientific-object";

    protected final String uriPath = path + "/get/{uri}";
    protected final String createPath = path + "/create";
    protected final String updatePath = path + "/update";
    protected final String deletePath = path + "/delete/{uri}";
    private final int soCount = 0;

    protected ScientificObjectDescriptionDTO getCreationDTO() throws Exception {
        ExperimentCreationDTO xpDto = new ExperimentCreationDTO();
        xpDto.setLabel("xp");

        LocalDate currentDate = LocalDate.now();
        xpDto.setStartDate(currentDate.minusDays(3));
        xpDto.setEndDate(currentDate.plusDays(3));
        xpDto.setCampaign(currentDate.getYear());
        xpDto.setObjective("Objective");

        final Response postResult = getJsonPostResponse(target(createPath), xpDto);

        // ensure that the result is a well formed URI, else throw exception
        URI createdUri = extractUriFromResponse(postResult);

        ScientificObjectDescriptionDTO dto = new ScientificObjectDescriptionDTO();
        List<Position> list = new LinkedList<>();
        list.add(new Position(3.97167246, 43.61328981));
        list.add(new Position(3.97171243, 43.61332417));
        list.add(new Position(3.9717427, 43.61330558));
        list.add(new Position(3.97170272, 43.61327122));
        list.add(new Position(3.97167246, 43.61328981));
        list.add(new Position(3.97167246, 43.61328981));
        Geometry geometry = new Polygon(list);

        dto.setName("SO " + soCount);
        dto.setType(new URI("vocabulary:Plot"));
        dto.setExperiment(createdUri);
        dto.setGeometry(geometryToGeoJson(geometry));

        return dto;
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
    public void testUpdate() throws Exception {
        // create the so
        ScientificObjectDescriptionDTO soDTO = getCreationDTO();
        final Response postResult = getJsonPostResponse(target(createPath), soDTO);

        // update the so
        soDTO.setUri(extractUriFromResponse(postResult));
        soDTO.setName("new alias");
        soDTO.setType(new URI("vocabulary:Leaf"));
        Geometry geometry = new Point(new Position(3.97167246, 43.61328981));
        soDTO.setGeometry(geometryToGeoJson(geometry));

        final Response updateResult = getJsonPutResponse(target(updatePath), soDTO);
        assertEquals(Status.OK.getStatusCode(), updateResult.getStatus());

        // retrieve the new scientific object and compare it to the expected scientific object
        final Response getResult = getJsonGetByUriResponse(target(uriPath), soDTO.getUri().toString());

        // try to deserialize object
        JsonNode node = getResult.readEntity(JsonNode.class);
        SingleObjectResponse<ScientificObjectDescriptionDTO> getResponse = mapper.convertValue(node, new TypeReference<SingleObjectResponse<ScientificObjectDescriptionDTO>>() {
        });
        ScientificObjectDescriptionDTO dtoFromApi = getResponse.getResult();

        // check that the object has been updated
        assertEquals(soDTO.getName(), dtoFromApi.getName());
        assertEquals(soDTO.getType(), dtoFromApi.getType());
        assertEquals(soDTO.getGeometry(), dtoFromApi.getGeometry());
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
    public void testGetDetail() throws Exception {
        final Response postResult = getJsonPostResponse(target(createPath), getCreationDTO());
        URI uri = extractUriFromResponse(postResult);

        final Response getResult = getJsonGetByUriResponse(target(uriPath), uri.toString());
        assertEquals(Status.OK.getStatusCode(), getResult.getStatus());

        // try to deserialize object
        JsonNode node = getResult.readEntity(JsonNode.class);
        SingleObjectResponse<ScientificObjectDetailDTO> getResponse = mapper.convertValue(node, new TypeReference<SingleObjectResponse<ScientificObjectDetailDTO>>() {
        });
        ScientificObjectDetailDTO soGetDetailDTO = getResponse.getResult();
        assertNotNull(soGetDetailDTO);
    }
}

package org.opensilex.faidare.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.opensilex.core.organisation.api.FacilityApiTest;
import org.opensilex.core.organisation.api.facility.FacilityCreationDTO;
import org.opensilex.faidare.model.Faidarev1LocationDTO;
import org.opensilex.server.rest.serialization.ObjectMapperContextResolver;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertFalse;

public class LocationsAPITest extends FacilityApiTest {

    public static final String path = "/faidare/v1/locations";



    protected void testSearchParams(Map<String, Object> params) throws Exception {
        WebTarget searchTarget = appendSearchParams(target(path), 0, 20, params);
        final Response getResult = appendAdminToken(searchTarget).get();
        assertEquals(Response.Status.OK.getStatusCode(), getResult.getStatus());

        JsonNode node = getResult.readEntity(JsonNode.class);
        ObjectMapper mapper = ObjectMapperContextResolver.getObjectMapper();
        List<Faidarev1LocationDTO> faidarev1LocationDTOList = mapper.convertValue(node.get("result").get("data"), new TypeReference<>() {
        });

        assertFalse(faidarev1LocationDTOList.isEmpty());
    }

    @Test
    public void testSearch() throws Exception {
        FacilityCreationDTO facilityCreationDTO = getCreationDTO(1);
        Response creationResponse = getJsonPostResponseAsAdmin(target(CREATE_PATH), facilityCreationDTO);
        assertEquals(Response.Status.CREATED.getStatusCode(), creationResponse.getStatus());

        Map<String, Object> params = new HashMap<>();
        testSearchParams(params);

        params.put("locationDbId", extractUriFromResponse(creationResponse));
        testSearchParams(params);
    }
}

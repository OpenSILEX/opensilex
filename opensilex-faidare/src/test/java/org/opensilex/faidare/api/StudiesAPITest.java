package org.opensilex.faidare.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;
import org.opensilex.brapi.BrapiPaginatedListResponse;
import org.opensilex.core.experiment.api.ExperimentCreationDTO;
import org.opensilex.integration.test.ServiceDescription;
import org.opensilex.integration.test.security.AbstractSecurityIntegrationTest;
import org.opensilex.security.person.api.PersonDTO;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class StudiesAPITest extends FaidareAPITest {

    protected static ServiceDescription search;

    static {
        try {
            search = new ServiceDescription(
                    StudiesAPI.class.getMethod(
                            "getStudiesList",
                            URI.class, String.class, String.class, String.class, int.class, int.class
                    ),
                    "/faidare/v1/studies"
            );
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testGet() throws Exception {
        BrapiPaginatedListResponse<JsonNode> deserializedSearchResult = new AbstractSecurityIntegrationTest.UserCallBuilder(search)
                .buildAdmin()
                .executeCallAndDeserialize(new TypeReference<BrapiPaginatedListResponse<JsonNode>>(){})
                .getDeserializedResponse();
        assertEquals(5, deserializedSearchResult.getResult().getData().size());
    }

    @Test
    public void testGetByUri() throws Exception {
        ExperimentCreationDTO experimentCreationDTO1 = experimentBuilder.getDTOList().get(0);
        BrapiPaginatedListResponse<JsonNode> deserializedSearchResult = new UserCallBuilder(search)
                .addParam("studyDbId", experimentCreationDTO1.getUri())
                .buildAdmin()
                .executeCallAndDeserialize(new TypeReference<BrapiPaginatedListResponse<JsonNode>>(){})
                .getDeserializedResponse();

        assertEquals(1, deserializedSearchResult.getResult().getData().size());

        // Check first level mapping
        Map<String, String> keysMatching = new HashMap<>(){{
            put("name", "studyName");
            put("start_date", "startDate");
            put("end_date", "endDate");
            put("description", "studyDescription");
        }};
        JsonNode expected = mapper.convertValue(experimentCreationDTO1, JsonNode.class);
        JsonNode actual = deserializedSearchResult.getResult().getData().get(0);
        assertTrue(valuesMatch(
                expected,
                actual,
                keysMatching
        ));

        assertEquals(expected.get("name"), actual.get("name"));
        assertEquals(expected.get("projects").get(0), actual.get("trialDbIds").get(0));
        assertEquals(expected.get("facilities").get(0), actual.get("locationDbId"));

        // Check deeper level mapping
        assertEquals(expected.get("scientific_supervisors").get(0), actual.get("contacts").get(0).get("contactDbId"));
        assertEquals(expected.get("technical_supervisors").get(0), actual.get("contacts").get(1).get("contactDbId"));

        JsonNode actualContact = actual.get("contacts").get(0);
        PersonDTO expectedContact = personBuilder.getDTOList().get(0);

        assertEquals(expectedContact.getUri().toString(), actualContact.get("contactDbId").asText());
        assertEquals(expectedContact.getEmail(), actualContact.get("email").asText());
        assertEquals(expectedContact.getAffiliation(), actualContact.get("institutionName").asText());
        String fullName = expectedContact.getLastName().toUpperCase()
                + " "
                + expectedContact.getFirstName().substring(0,1).toUpperCase()
                + expectedContact.getFirstName().substring(1);
        assertEquals(fullName, actualContact.get("name").asText());
        assertEquals("ScientificSupervisor", actualContact.get("type").asText());
        assertEquals("TechnicalSupervisor", actual.get("contacts").get(1).get("type").asText());

    }

}

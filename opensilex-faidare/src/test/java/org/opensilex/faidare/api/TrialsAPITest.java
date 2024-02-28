package org.opensilex.faidare.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;
import org.opensilex.brapi.BrapiPaginatedListResponse;
import org.opensilex.core.project.api.ProjectCreationDTO;
import org.opensilex.integration.test.ServiceDescription;
import org.opensilex.integration.test.security.AbstractSecurityIntegrationTest;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TrialsAPITest  extends FaidareAPITest {

    protected static ServiceDescription search;

    static {
        try {
            search = new ServiceDescription(
                    TrialsAPI.class.getMethod("getTrialsList", int.class, int.class),
                    "/faidare/v1/trials"
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

        ProjectCreationDTO projectCreationDTO = projectBuilder.getDTOList().get(0);

        // Check first level mapping
        Map<String, String> keysMatching = new HashMap<>(){{
            put("name", "trialName");
            put("start_date", "startDate");
            put("end_date", "endDate");
            put("website", "documentationURL");
            put("objective", "trialType");
        }};
        JsonNode expected = mapper.convertValue(projectCreationDTO, JsonNode.class);
        JsonNode actual = deserializedSearchResult.getResult().getData().get(0);
        assertTrue(valuesMatch(
                expected,
                actual,
                keysMatching
        ));

        // Check deeper level mapping
        assertEquals(experimentBuilder.getDTOList().get(0).getUri().toString(), actual.get("studies").get(0).get("studyDbId").asText());
        assertEquals(expected.get("administrative_contacts").get(0), actual.get("contacts").get(0).get("contactDbId"));
        assertEquals(expected.get("shortname"), actual.get("additionalInfo").get("shortName"));
        assertEquals(expected.get("description"), actual.get("additionalInfo").get("description"));
        assertEquals(expected.get("financial_funding"), actual.get("additionalInfo").get("financialFunding"));
        assertEquals(expected.get("related_projects"), actual.get("additionalInfo").get("relatedProjects"));
        assertEquals(expected.get("coordinators").get(0), actual.get("additionalInfo").get("coordinators").get(0).get("contactDbId"));

    }
}

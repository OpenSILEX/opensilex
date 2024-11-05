/*
 * *****************************************************************************
 *                         TrialsAPITest.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2024.
 * Last Modification: 25/05/2024 23:10
 * Contact: gabriel.besombes@inrae.fr
 * *****************************************************************************
 */

package org.opensilex.faidare.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;
import org.opensilex.brapi.BrapiPaginatedListResponse;
import org.opensilex.core.experiment.api.ExperimentCreationDTO;
import org.opensilex.core.organisation.api.facility.FacilityCreationDTO;
import org.opensilex.core.project.api.ProjectCreationDTO;
import org.opensilex.integration.test.ServiceDescription;
import org.opensilex.integration.test.security.AbstractSecurityIntegrationTest;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TrialsAPITest  extends FaidareAPITest {

    protected static final ServiceDescription search;

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
        assertTrue(SPARQLDeserializers.compareURIs(experimentBuilder.getDTOList().get(0).getUri().toString(), actual.get("studies").get(0).get("studyDbId").asText()));
        assertTrue(SPARQLDeserializers.compareURIs(expected.get("administrative_contacts").get(0).asText(), actual.get("contacts").get(0).get("contactDbId").asText()));
        assertEquals(expected.get("shortname"), actual.get("additionalInfo").get("shortName"));
        assertEquals(expected.get("description"), actual.get("additionalInfo").get("description"));
        assertEquals(expected.get("financial_funding"), actual.get("additionalInfo").get("financialFunding"));
        assertEquals(expected.get("related_projects"), actual.get("additionalInfo").get("relatedProjects"));
        assertTrue(SPARQLDeserializers.compareURIs(expected.get("coordinators").get(0).asText(), actual.get("additionalInfo").get("coordinators").get(0).get("contactDbId").asText()));

        FacilityCreationDTO expectedLocation = facilityBuilder.getDTOList().get(0);
        ExperimentCreationDTO expectedStudy = experimentBuilder.getDTOList().get(0);
        assertTrue(SPARQLDeserializers.compareURIs(expectedLocation.getUri().toString(), actual.get("studies").get(0).get("locationDbId").asText()));
        assertEquals(expectedLocation.getName(), actual.get("studies").get(0).get("locationName").asText());
        assertTrue(SPARQLDeserializers.compareURIs(expectedStudy.getUri().toString(), actual.get("studies").get(0).get("studyDbId").asText()));
        assertEquals(expectedStudy.getName(), actual.get("studies").get(0).get("studyName").asText());

    }
}

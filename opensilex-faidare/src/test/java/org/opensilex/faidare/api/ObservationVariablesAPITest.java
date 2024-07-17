package org.opensilex.faidare.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;
import org.opensilex.brapi.BrapiPaginatedListResponse;
import org.opensilex.core.variable.api.VariableCreationDTO;
import org.opensilex.core.variable.api.unit.UnitCreationDTO;
import org.opensilex.integration.test.ServiceDescription;
import org.opensilex.integration.test.security.AbstractSecurityIntegrationTest;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ObservationVariablesAPITest extends FaidareAPITest {

    protected static final ServiceDescription search;

    static {
        try {
            search = new ServiceDescription(
                    ObservationVariablesAPI.class.getMethod("getVariablesList", URI.class, int.class, int.class),
                    "/faidare/v1/variables"
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
        VariableCreationDTO variableCreationDTO = variableBuilder.getDTOList().get(0);
        BrapiPaginatedListResponse<JsonNode> deserializedSearchResult = new UserCallBuilder(search)
                .addParam("observationVariableDbId", variableCreationDTO.getUri())
                .buildAdmin()
                .executeCallAndDeserialize(new TypeReference<BrapiPaginatedListResponse<JsonNode>>(){})
                .getDeserializedResponse();

        assertEquals(1, deserializedSearchResult.getResult().getData().size());

        // Check first level mapping
        Map<String, String> keysMatching = new HashMap<>(){{
            put("name", "name");
        }};
        JsonNode expected = mapper.convertValue(variableCreationDTO, JsonNode.class);
        JsonNode actual = deserializedSearchResult.getResult().getData().get(0);
        assertTrue(valuesMatch(
                expected,
                actual,
                keysMatching
        ));

        // Check deeper level mapping
        assertEquals(expected.get("alternative_name"), actual.get("synonyms").get(0));

        // Check Method
        Map<String, String> methodKeysMatching = new HashMap<>(){{
            put("name", "name");
            put("uri", "methodDbId");
            put("description", "description");
        }};
        JsonNode expectedMethod = mapper.convertValue(methodBuilder.getDTOList().get(0), JsonNode.class);
        JsonNode actualMethod = actual.get("method");
        assertTrue(valuesMatch(
                expectedMethod,
                actualMethod,
                methodKeysMatching
        ));

        // Check Scale
        Map<String, String> scaleKeysMatching = new HashMap<>(){{
            put("name", "name");
            put("uri", "scaleDbId");
        }};
        UnitCreationDTO unitDTO = unitBuilder.getDTOList().get(0);
        JsonNode expectedScale = mapper.convertValue(unitDTO, JsonNode.class);
        JsonNode actualScale = actual.get("scale");
        assertTrue(valuesMatch(
                expectedScale,
                actualScale,
                scaleKeysMatching
        ));
        assertEquals("Numerical", actualScale.get("dataType").asText());

        // Check Trait
        Map<String, String> traitKeysMatching = new HashMap<>(){{
            put("trait_name", "name");
            put("trait", "traitDbId");
        }};
        JsonNode actualTrait = actual.get("trait");
        assertTrue(valuesMatch(
                expected,
                actualTrait,
                traitKeysMatching
        ));
        assertEquals(entityBuilder.getDTOList().get(0).getName(), actualTrait.get("entity").asText());
        assertEquals(characteristicBuilder.getDTOList().get(0).getName(), actualTrait.get("attribute").asText());
    }
}

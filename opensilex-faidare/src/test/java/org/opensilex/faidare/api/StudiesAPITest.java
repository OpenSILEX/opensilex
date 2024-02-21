package org.opensilex.faidare.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;
import org.opensilex.brapi.BrapiPaginatedListResponse;
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.core.experiment.api.ExperimentAPITest;
import org.opensilex.core.experiment.api.ExperimentCreationDTO;
import org.opensilex.core.experiment.api.ExperimentGetListDTO;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.organisation.api.facility.FacilityCreationDTO;
import org.opensilex.core.organisation.dal.OrganizationDAO;
import org.opensilex.core.organisation.dal.facility.FacilityDAO;
import org.opensilex.faidare.builder.Faidarev1StudyDTOBuilder;
import org.opensilex.faidare.model.Faidarev1StudyDTO;
import org.opensilex.faidare.responses.Faidarev1StudyListResponse;
import org.opensilex.integration.test.ServiceDescription;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.authentication.injection.CurrentUser;
import org.opensilex.server.response.JsonResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;

import javax.inject.Inject;
import java.net.URI;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class StudiesAPITest  extends FaidareAPITest{

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
    public void testGetByUri() throws Exception {
        ExperimentCreationDTO experimentCreationDTO1 = TestExperimentBuilder.getDTOList().get(0);
        BrapiPaginatedListResponse<JsonNode> deserializedSearchResult = new UserCallBuilder(search)
                .addParam("studyDbId", experimentCreationDTO1.getUri())
                .buildAdmin()
                .executeCallAndDeserialize(new TypeReference<BrapiPaginatedListResponse<JsonNode>>(){})
                .getDeserializedResponse();

        assertEquals(1, deserializedSearchResult.getResult().getData().size());

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
        assertEquals(expected.get("facilities").get(0), actual.get("location").get("locationDbId"));
    }

}

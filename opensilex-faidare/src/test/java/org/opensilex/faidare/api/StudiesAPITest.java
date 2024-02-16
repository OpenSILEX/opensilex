package org.opensilex.faidare.api;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.Test;
import org.opensilex.brapi.BrapiPaginatedListResponse;
import org.opensilex.core.experiment.api.ExperimentAPITest;
import org.opensilex.core.experiment.api.ExperimentCreationDTO;
import org.opensilex.core.experiment.api.ExperimentGetListDTO;
import org.opensilex.faidare.model.Faidarev1StudyDTO;
import org.opensilex.faidare.responses.Faidarev1StudyListResponse;
import org.opensilex.server.response.JsonResponse;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.utils.ListWithPagination;

import java.net.URI;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class StudiesAPITest extends ExperimentAPITest {

    protected static ServiceDescription search;

    static {
        try {
            search = new ServiceDescription(
                    StudiesAPI.class.getMethod(
                            "getStudies",
                            URI.class, String.class, String.class, String.class, int.class, int.class
                    ),
                    "/faidare/v1/studies"
            );
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testSearch() throws Exception {
        ExperimentCreationDTO experimentDTO = getCreationDTO();
        URI experimentUri = new UserCallBuilder(create).setBody(experimentDTO).buildAdmin().executeCallAndReturnURI();

        LinkedHashMap<String, Object> attributesToCheck = new LinkedHashMap<>(){{
            put("studyDbId", experimentUri.toString());
            put("studyName", DEFAULT_EXPERIMENT_NAME);
            put("startDate", DEFAULT_EXPERIMENT_START_DATE.toString());
            put("endDate", DEFAULT_EXPERIMENT_END_DATE.toString());
        }};
        BrapiPaginatedListResponse<Faidarev1StudyDTO> deserializedSearchResult = new UserCallBuilder(search)
                .addParam("studyDbId", experimentUri)
                .buildAdmin()
                .executeCallAndDeserialize(new TypeReference<BrapiPaginatedListResponse<Faidarev1StudyDTO>>(){})
                .getDeserializedResponse();

        assertEquals(deserializedSearchResult.getResult().getData().size(), 1);
        assertTrue(isDeepMapIncluded(convertToNotNullNestedMap(deserializedSearchResult.getResult().getData().get(0)), attributesToCheck));
    }

}

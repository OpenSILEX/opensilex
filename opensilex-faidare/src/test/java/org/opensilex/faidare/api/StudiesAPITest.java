package org.opensilex.faidare.api;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.Test;
import org.opensilex.core.experiment.api.ExperimentAPITest;
import org.opensilex.faidare.responses.Faidarev1StudyListResponse;

import java.net.URI;
import java.util.HashMap;

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

    // TODO : validate more fields
    @Test
    public void testSearch() throws Exception {
        URI experimentUri = new UserCallBuilder(create).setBody(getCreationDTO()).buildAdmin().executeCallAndReturnURI();

        HashMap<String, Object> params = new HashMap<>() {{
            put("studyDbId", experimentUri);
        }};

        Faidarev1StudyListResponse studiesSearchResponse = new UserCallBuilder(search).setParams(params).buildAdmin().executeCallAndDeserialize(new TypeReference<Faidarev1StudyListResponse>() {
        }).getDeserializedResponse();
    }

}

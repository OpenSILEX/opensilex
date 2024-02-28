package org.opensilex.faidare.api;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.Ignore;
import org.junit.Test;
import org.opensilex.core.germplasm.api.BaseGermplasmAPITest;
import org.opensilex.core.germplasm.api.GermplasmGetSingleDTO;
import org.opensilex.faidare.responses.Faidarev1GermplasmListResponse;
import org.opensilex.integration.test.ServiceDescription;
import org.opensilex.server.response.SingleObjectResponse;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class GermplasmAPITest extends BaseGermplasmAPITest {

    public static final String path = "/faidare/v1/germplasm";
    protected static final ServiceDescription search;

    static {
        try {
            search = new ServiceDescription(
                    GermplasmAPI.class.getMethod(
                            "getGermplasmBySearch",
                            URI.class, URI.class, String.class, String.class, int.class, int.class
                    ),
                    path
            );
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Ignore("Issues with modules hierarchy and loading of the 'Accession' notion")
    @Test
    public void testSearch() throws Exception {

        URI speciesUri = createSpecies();
        SingleObjectResponse<GermplasmGetSingleDTO> speciesGetResponse = new UserCallBuilder(get).setUriInPath(speciesUri).buildAdmin().executeCallAndDeserialize(new TypeReference<SingleObjectResponse<GermplasmGetSingleDTO>>() {
        }).getDeserializedResponse();
        URI varietyUri = createVariety(speciesUri);
        URI accessionUri = createAccession(varietyUri);

        Map<String, Object> params = new HashMap<>() {
            {
                put("germplasmDbId", accessionUri);
                put("commonCropName", speciesGetResponse.getResult().getName());
            }
        };

        new UserCallBuilder(search).setParams(params).buildAdmin().executeCallAndDeserialize(new TypeReference<Faidarev1GermplasmListResponse>() {
        });
    }
}

package org.opensilex.faidare.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Ignore;
import org.junit.Test;
import org.opensilex.core.germplasm.api.BaseGermplasmAPITest;
import org.opensilex.core.germplasm.api.GermplasmGetSingleDTO;
import org.opensilex.faidare.responses.Faidarev1GermplasmListResponse;
import org.opensilex.integration.test.ServiceDescription;
import org.opensilex.integration.test.security.AbstractSecurityIntegrationTest;
import org.opensilex.server.response.SingleObjectResponse;

import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class GermplasmAPITest extends FaidareAPITest {
    protected static final ServiceDescription search;

    static {
        try {
            search = new ServiceDescription(
                    GermplasmAPI.class.getMethod(
                            "getGermplasmsBySearch", URI.class, URI.class, String.class, String.class, int.class, int.class),
                    "/faidare/v1/germplasm"
            );
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testSearch() throws Exception {

        // There are currently issues with the "accession" notion being redefined in multiple modules (PHIS, Sixtine, etc.)
        // This makes it so that the notion doesn't exist in the testing environment so the service should be unavailable.
        new UserCallBuilder(search).buildAdmin().executeCallAndAssertStatus(Response.Status.SERVICE_UNAVAILABLE);
    }
}

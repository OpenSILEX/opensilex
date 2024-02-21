package org.opensilex.faidare.api;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.opensilex.core.AbstractMongoIntegrationTest;
import org.opensilex.core.experiment.api.ExperimentAPITest;
import org.opensilex.core.experiment.api.ExperimentCreationDTO;
import org.opensilex.core.organisation.api.FacilityApiTest;
import org.opensilex.core.organisation.api.facility.FacilityCreationDTO;

import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class FaidareAPITest extends AbstractMongoIntegrationTest {

    public boolean valuesMatch(JsonNode expected, JsonNode actual, Map<String, String> keysMatching) {
        for (Map.Entry<String, String> entry : keysMatching.entrySet()) {
            if (!expected.has(entry.getKey()) || !expected.get(entry.getKey()).equals(actual.get(entry.getValue()))) {
                return false;
            }
        }
        return true;
    }
    @Before
    public void globalFaidareSetUp() throws Exception {
        TestFacilityBuilder facilityBuilder = new TestFacilityBuilder();
        for (int i=0; i<5; i++) {
            new UserCallBuilder(FacilityApiTest.create)
                    .setBody(facilityBuilder.createDTO())
                    .buildAdmin()
                    .executeCallAndAssertStatus(Response.Status.CREATED);
        }

        TestExperimentBuilder experimentBuilder = new TestExperimentBuilder();
        for (int i=0; i<5; i++) {
            experimentBuilder.setFacilities(List.of(TestFacilityBuilder.getDTOList().get(i).getUri()));
            new UserCallBuilder(ExperimentAPITest.create)
                    .setBody(experimentBuilder.createDTO())
                    .buildAdmin()
                    .executeCallAndAssertStatus(Response.Status.CREATED);
        }
    }
}

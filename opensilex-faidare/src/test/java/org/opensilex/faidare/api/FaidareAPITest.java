package org.opensilex.faidare.api;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.opensilex.core.AbstractMongoIntegrationTest;

import java.net.URI;


@RunWith(Suite.class)
@Suite.SuiteClasses({
        TrialsAPITest.class,
        VariablesAPITest.class
})
public class FaidareAPITest extends AbstractMongoIntegrationTest {


    @BeforeClass
    public static void globalFaidareSetUp() throws Exception {
        URI experimentURI = new TestExperimentBuilder().create();
    }
}

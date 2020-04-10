/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.unit.test;

import java.util.HashMap;
import java.util.Map;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.opensilex.OpenSilex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author vmigot
 */
public class AbstractUnitTest {

    protected final static Logger LOGGER = LoggerFactory.getLogger(AbstractUnitTest.class);

    @Rule
    public TestRule watcher = new TestWatcher() {
        protected void starting(Description description) {
            LOGGER.debug("\n\n####### Starting UT: " + description.getTestClass().getSimpleName() + " - " + description.getMethodName() + " #######");
        }
    };

    protected static OpenSilex opensilex;

    public static OpenSilex getOpensilex() {
        return opensilex;
    }

    @BeforeClass
    public static void createOpenSilex() throws Exception {
        Map<String, String> args = new HashMap<>();
        args.put(OpenSilex.PROFILE_ID_ARG_KEY, OpenSilex.TEST_PROFILE_ID);
        args.put(OpenSilex.NO_CACHE_ARG_KEY, "true");
//        args.put(OpenSilex.DEBUG_ARG_KEY, "true");

        LOGGER.debug("Create OpenSilex instance for Unit Test (setup only)");
        opensilex = OpenSilex.createStaticInstance(args);
    }

    @AfterClass
    public static void stopOpenSilex() throws Exception {
        opensilex.shutdown();
    }

}

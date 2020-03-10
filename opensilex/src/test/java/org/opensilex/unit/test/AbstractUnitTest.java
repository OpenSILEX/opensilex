/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.unit.test;

import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author vmigot
 */
public class AbstractUnitTest {

    private final static Logger LOGGER = LoggerFactory.getLogger(AbstractUnitTest.class);
    
    @Rule
    public TestRule watcher = new TestWatcher() {
        protected void starting(Description description) {
            LOGGER.debug("\n####### Starting UT: " + description.getTestClass().getSimpleName() + " - " + description.getMethodName() + " #######");
        }
    };
}

package org.opensilex.core;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensilex.sparql.exceptions.SPARQLQueryException;
import org.opensilex.sparql.service.SPARQLService;

import java.net.URISyntaxException;
import java.util.List;

/**
 * Abstract class used for DAO testing
 * @author Renaud COLIN
 */
public abstract class AbstractDaoTest {

    protected static OpenSilexTestContext context;
    protected static SPARQLService service;

    @BeforeClass
    public static void setup() throws Exception {
       context = new OpenSilexTestContext();
       service = context.getSparqlService();
    }

    @AfterClass
    public static void end() throws Exception {
        context.shutdown();
    }

    @After
    public void clearGraph() throws SPARQLQueryException, URISyntaxException {
        context.clearGraphs(getGraphsToCleanNames());
    }

    /**
     *
     * @return
     */
    protected abstract List<String> getGraphsToCleanNames();

}

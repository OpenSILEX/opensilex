package org.opensilex.sparql.utils;

import org.opensilex.OpenSilex;
import org.opensilex.sparql.exceptions.SPARQLInvalidClassDefinitionException;
import org.opensilex.sparql.model.*;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.service.SPARQLServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class used for building an instance of {@link OpenSilex} for tests.
 * @author rcolin
 */
public class OpenSilexTestEnvironment {

    private static OpenSilexTestEnvironment INSTANCE;

    protected static final Logger LOGGER = LoggerFactory.getLogger(OpenSilexTestEnvironment.class);

    private final OpenSilex openSilex;
    private final SPARQLService sparql;
    private final SPARQLServiceFactory sparqlServiceFactory;

    public static OpenSilexTestEnvironment getInstance() throws Exception {
        if (INSTANCE == null) {
            INSTANCE = new OpenSilexTestEnvironment();
        }
        return INSTANCE;
    }

    public OpenSilexTestEnvironment() throws Exception {

        Map<String, String> args = new HashMap<>();
        args.put(OpenSilex.PROFILE_ID_ARG_KEY, OpenSilex.TEST_PROFILE_ID);
        args.put(OpenSilex.NO_CACHE_ARG_KEY, "true");

        // NOTE: uncomment this line to enable full debug during unit tests
//        args.put(OpenSilex.DEBUG_ARG_KEY, "true");
        LOGGER.debug("Create OpenSilex instance for Unit Test");
        openSilex = OpenSilex.createInstance(args);

        sparqlServiceFactory = openSilex.getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class);
        sparqlServiceFactory.getMapperIndex().addClasses(
                A.class,
                B.class,
                C.class
        );
        sparql = openSilex.getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class).provide();
    }

    public OpenSilex getOpenSilex() {
        return openSilex;
    }

    public SPARQLService getSparql() {
        return sparql;
    }

    public void stopOpenSilex() throws Exception {
        openSilex.shutdown();
    }

    public void addTestClasses(List<Class<? extends SPARQLResourceModel>> newClasses) throws SPARQLInvalidClassDefinitionException {
        for(var clazz : newClasses) {
            sparqlServiceFactory.getMapperIndex().addClasses(clazz);
        }
    }

}

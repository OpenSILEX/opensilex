package org.opensilex.sparql.utils;

import org.opensilex.OpenSilex;
import org.opensilex.sparql.model.A;
import org.opensilex.sparql.model.B;
import org.opensilex.sparql.model.C;
import org.opensilex.sparql.rdf4j.RDF4JInMemoryServiceFactory;
import org.opensilex.sparql.service.SPARQLService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class used for building an instance of {@link OpenSilex} for tests.
 * @author rcolin
 */
public class OpenSilexTestEnvironment {

    private static OpenSilexTestEnvironment INSTANCE;

    protected final static Logger LOGGER = LoggerFactory.getLogger(OpenSilexTestEnvironment.class);

    private final OpenSilex openSilex;
    private final RDF4JInMemoryServiceFactory factory;
    private final SPARQLService sparql;

    public static OpenSilexTestEnvironment getInstance() throws Exception {
        if (INSTANCE == null) {
            INSTANCE = new OpenSilexTestEnvironment();
        }
        return INSTANCE;
    }

    private OpenSilexTestEnvironment() throws Exception {

        Map<String, String> args = new HashMap<>();
        args.put(OpenSilex.PROFILE_ID_ARG_KEY, OpenSilex.TEST_PROFILE_ID);
        args.put(OpenSilex.NO_CACHE_ARG_KEY, "true");

        // NOTE: uncomment this line to enable full debug during unit tests
//        args.put(OpenSilex.DEBUG_ARG_KEY, "true");
        LOGGER.debug("Create OpenSilex instance for Unit Test");
        openSilex = OpenSilex.createInstance(args);

        factory = new RDF4JInMemoryServiceFactory();
        factory.setOpenSilex(openSilex);
        factory.setup();
        factory.startup();
        factory.getMapperIndex().addClasses(
                A.class,
                B.class,
                C.class
        );

        sparql = factory.provide();
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

    public void cleanSPARQL() {
        factory.dispose(sparql);
    }

}

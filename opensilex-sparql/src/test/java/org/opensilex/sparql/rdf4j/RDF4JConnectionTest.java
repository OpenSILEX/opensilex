//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql.rdf4j;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.opensilex.sparql.SPARQLServiceTest;
import org.opensilex.sparql.model.*;

/**
 *
 * @author vincent
 */
public class RDF4JConnectionTest extends SPARQLServiceTest {

    /**
     * Prefix only defined in the repository namespaces
     */
    public static final String TEST_PREFIX_IN_REPOSITORY = "prefix-in-repository";
    /**
     * Namespace corresponding to {@link RDF4JConnectionTest#TEST_NAMESPACE_IN_REPOSITORY}
     */
    public static final String TEST_NAMESPACE_IN_REPOSITORY = "https://example.org/namespace-in-repository#";

    protected static RDF4JInMemoryServiceFactory factory;

    @BeforeClass
    public static void setupSPARQL() throws Exception {
        factory = new RDF4JInMemoryServiceFactory();
        setupTestRepository(factory.getRepository());
        factory.setOpenSilex(opensilex);
        factory.setup();
        factory.startup();
        factory.getMapperIndex().addClasses(
                A.class,
                B.class,
                C.class,
                D.class,
                UriGeneratedTestModel.class
        );
        sparql = factory.provide();
        SPARQLServiceTest.initialize();
    }

    private static void setupTestRepository(Repository repository) {
        // Add test prefix in repository
        try (RepositoryConnection connection = factory.getRepository().getConnection()) {
            connection.setNamespace(TEST_PREFIX_IN_REPOSITORY, TEST_NAMESPACE_IN_REPOSITORY);
        }
    }

    @AfterClass
    public static void cleanSPARQL() throws Exception {
        factory.dispose(sparql);
    }
}

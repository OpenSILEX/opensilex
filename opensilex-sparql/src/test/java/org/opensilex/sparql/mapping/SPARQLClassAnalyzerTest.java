package org.opensilex.sparql.mapping;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensilex.sparql.exceptions.SPARQLInvalidClassDefinitionException;
import org.opensilex.sparql.rdf4j.RDF4JConnection;
import org.opensilex.sparql.service.SPARQLService;
import test.opensilex.sparql.SPARQLServiceTest;

public class SPARQLClassAnalyzerTest {

    @BeforeClass
    public static void initialize() throws Exception {

        // force inclusion of these two classes from the SPARQLClassObjectMapper initialisation
        SPARQLClassObjectMapper.includeResourceClass(NoGetterClass.class);
        SPARQLClassObjectMapper.includeResourceClass(NoSetterClass.class);

        Repository repository = new SailRepository(new MemoryStore());
        repository.init();

        SPARQLService localService = new SPARQLService(new RDF4JConnection(repository.getConnection()));
        SPARQLServiceTest.initialize(localService);
    }

    @AfterClass
    public static void shutdown() throws Exception {
        SPARQLClassObjectMapper.reset();
        SPARQLServiceTest.destroy();
    }

    @Test(expected = SPARQLInvalidClassDefinitionException.class)
    public void testNoGetterClass() throws SPARQLInvalidClassDefinitionException {
        NoGetterClass c = new NoGetterClass();
        new SPARQLClassAnalyzer(c.getClass());
    }

    @Test(expected = SPARQLInvalidClassDefinitionException.class)
    public void testNoSetterClass() throws SPARQLInvalidClassDefinitionException {
        NoSetterClass c = new NoSetterClass();
        new SPARQLClassAnalyzer(c.getClass());
    }

}

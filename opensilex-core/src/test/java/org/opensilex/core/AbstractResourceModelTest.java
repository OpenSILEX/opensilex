package org.opensilex.core;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.shacl.ShaclSail;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.opensilex.sparql.SPARQLModule;
import org.opensilex.sparql.exceptions.SPARQLQueryException;
import org.opensilex.sparql.mapping.NoGetterClass;
import org.opensilex.sparql.mapping.NoSetterClass;
import org.opensilex.sparql.mapping.SPARQLClassAnalyzer;
import org.opensilex.sparql.mapping.SPARQLClassObjectMapper;
import org.opensilex.sparql.rdf4j.RDF4JConnection;
import org.opensilex.sparql.service.SPARQLService;

import java.net.URISyntaxException;
import java.util.List;

/**
 * @author Renaud COLIN
 */
public abstract class AbstractResourceModelTest {

    protected static Repository repository;
    protected static SPARQLService service;

    @BeforeClass
    public static void initialize() throws Exception {

        repository = new SailRepository(new ShaclSail(new MemoryStore()));
        repository.init();

        service = new SPARQLService(new RDF4JConnection(repository.getConnection()));
        service.startup();

        SPARQLClassObjectMapper.excludeResourceClass(NoGetterClass.class);
        SPARQLClassObjectMapper.excludeResourceClass(NoSetterClass.class);
    }

    @AfterClass
    public static void end() throws Exception {
        service.shutdown();
    }

    @After
    public void cleanGraph() throws URISyntaxException, SPARQLQueryException {

        for(String graphName : getGraphsToCleanNames()){
            service.clearGraph(SPARQLModule.getPlatformDomainGraphURI(graphName));
        }
        service.clearGraph(SPARQLModule.getPlatformURI());
    }

    protected abstract List<String> getGraphsToCleanNames();
}

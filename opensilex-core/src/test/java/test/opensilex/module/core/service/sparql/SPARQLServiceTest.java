/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test.opensilex.module.core.service.sparql;

import test.opensilex.module.core.service.sparql.model.TEST_ONTOLOGY;
import java.io.InputStream;
import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opensilex.module.core.service.sparql.SPARQLService;
import org.opensilex.module.core.service.sparql.SPARQLConnection;
import org.opensilex.module.core.service.sparql.exceptions.SPARQLQueryException;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author vincent
 */
public abstract class SPARQLServiceTest<T extends SPARQLConnection> {

    private SPARQLService service;

    protected abstract T createConnection();

    protected abstract void closeConnection();

    @Before
    public void initialize() throws SPARQLQueryException {
        service = new SPARQLService(createConnection());
        InputStream ontology = getClass().getClassLoader().getResourceAsStream(TEST_ONTOLOGY.FILE_PATH.toString());
        service.loadOntologyStream(TEST_ONTOLOGY.GRAPH, ontology, TEST_ONTOLOGY.FILE_FORMAT);
        
        InputStream ontologyData = getClass().getClassLoader().getResourceAsStream(TEST_ONTOLOGY.DATA_FILE_PATH.toString());
        service.loadOntologyStream(TEST_ONTOLOGY.DATA_GRAPH, ontologyData, TEST_ONTOLOGY.DATA_FILE_FORMAT);
    }

    @After
    public void destroy() throws SPARQLQueryException {
//        service.clearGraph(TEST_ONTOLOGY.GRAPH);
        closeConnection();
    }

    @Test
    public void testAskQuery() throws SPARQLQueryException {
        testClass(TEST_ONTOLOGY.A);
        testClass(TEST_ONTOLOGY.A1);
        testClass(TEST_ONTOLOGY.A2);
        testClass(TEST_ONTOLOGY.A11);
        testClass(TEST_ONTOLOGY.A21);
        testClass(TEST_ONTOLOGY.B);
    }
    
    private void testClass(Resource clazz) throws SPARQLQueryException {
        Node owlClassNode = NodeFactory.createURI(OWL.CLASS.stringValue());
        AskBuilder askQuery = new AskBuilder();
        askQuery.addWhere(clazz, RDF.type, owlClassNode);
        boolean classExists = service.executeAskQuery(askQuery);
        assertTrue("Class " + clazz.getLocalName() + " must exists in test ontology", classExists);
    }
}

//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package test.opensilex.sparql;

import java.io.InputStream;
import java.net.URI;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import static org.hamcrest.CoreMatchers.instanceOf;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.opensilex.OpenSilex;
import org.opensilex.sparql.SPARQLService;
import org.opensilex.sparql.exceptions.SPARQLQueryException;
import org.opensilex.sparql.mapping.SPARQLClassObjectMapper;
import test.opensilex.sparql.model.A;
import test.opensilex.sparql.model.B;
import test.opensilex.sparql.model.TEST_ONTOLOGY;


/**
 *
 * @author vincent
 */
public abstract class SPARQLServiceTest {

    protected static SPARQLService service;

    public static void initialize(SPARQLService service) throws Exception {
        SPARQLServiceTest.service = service;
        service.startup();

        SPARQLClassObjectMapper.initialize();
        
        service.clear();

        InputStream ontology = SPARQLService.class.getClassLoader().getResourceAsStream(TEST_ONTOLOGY.FILE_PATH.toString());
        service.loadOntologyStream(OpenSilex.getPlatformURI(), ontology, TEST_ONTOLOGY.FILE_FORMAT);

        InputStream ontologyData = SPARQLService.class.getClassLoader().getResourceAsStream(TEST_ONTOLOGY.DATA_FILE_PATH.toString());
        service.loadOntologyStream(OpenSilex.getPlatformURI("data"), ontologyData, TEST_ONTOLOGY.DATA_FILE_FORMAT);
    }

    @AfterClass
    public static void destroy() throws SPARQLQueryException {
        service.clearGraph(OpenSilex.getPlatformURI());
        service.clearGraph(OpenSilex.getPlatformURI("data"));
    }

    @Test
    public void testAskQuery() throws SPARQLQueryException {
        testClass(TEST_ONTOLOGY.A);
        testClass(TEST_ONTOLOGY.B);
    }

    private void testClass(Resource clazz) throws SPARQLQueryException {
        Node owlClassNode = NodeFactory.createURI(OWL.CLASS.stringValue());
        AskBuilder askQuery = new AskBuilder();
        askQuery.addWhere(clazz, RDF.type, owlClassNode);
        boolean classExists = service.executeAskQuery(askQuery);
        assertTrue("Class " + clazz.getLocalName() + " must exists in test ontology", classExists);
    }

    @Test
    public void testGetByURI() throws Exception {
        URI aURI = new URI("http://test.opensilex.org/a/001");
        A a = service.getByURI(A.class, aURI);

        assertEquals("Instance URI must be the same", aURI, a.getUri());

        B b = a.getB();

        assertNotNull("Instance object relation should exists", b);
        assertThat("b Must be an instance of B", b, instanceOf(B.class));

        URI bURI = new URI("http://test.opensilex.org/b/001");

        assertEquals("B Instance URI must be the same", bURI, b.getUri());
        assertEquals("A.getString Method should return the configured string", "azerty", a.getString());
        assertEquals("A.isBool Method should return the configured boolean", Boolean.TRUE, a.isBool());
        assertEquals("A.getByteVar Method should return the configured byte", Byte.valueOf((byte) 10), a.getByteVar());
        assertEquals("A.getCharVar Method should return the configured char", Character.valueOf('Z'), a.getCharVar());
        assertEquals("A.getDoubleVar Method should return the configured double", -24.005, a.getDoubleVar(), 0);
        assertEquals("A.getFloatVar Method should return the configured float", (float) 3.25, a.getFloatVar(), 0);
        assertEquals("A.getInteger Method should return the configured integer", Integer.valueOf(4), a.getInteger());
        assertEquals("A.getLongVar Method should return the configured long", Long.valueOf(-5), a.getLongVar());
        assertEquals("A.getShortVar Method should return the configured short", Short.valueOf((short) 8), a.getShortVar());

        LocalDate date = a.getDate();
        LocalDate expectedDate = LocalDate.of(2017, 12, 10);
        assertEquals("A.getDate Method should return the configured date", expectedDate, date);

        OffsetDateTime expectedDateTime = OffsetDateTime.of(2017, 5, 1, 9, 30, 10, 0, ZoneOffset.ofHours(6));
        OffsetDateTime dateTime = a.getDatetime();
        assertEquals("A.getDatetime Method should return the configured date time", expectedDateTime, dateTime);

        assertEquals("B.isBool Method should return the configured boolean", Boolean.FALSE, b.getBool());
        assertEquals("B.getByteVar Method should return the configured byte", Byte.valueOf((byte) -4), b.getByteVar());
        assertEquals("B.getCharVar Method should return the configured char", Character.valueOf('X'), b.getCharVar());
        assertEquals("B.getDoubleVar Method should return the configured double", 3.5, b.getDoubleVar(), 0);
        assertEquals("B.getFloatVar Method should return the configured float", (float) -2.425, b.getFloatVar(), 0);
        assertEquals("B.getInteger Method should return the configured integer", Integer.valueOf(-6), b.getInteger());
        assertEquals("B.getLongVar Method should return the configured long", Long.valueOf(9), b.getLongVar());
        assertEquals("B.getShortVar Method should return the configured short", Short.valueOf((short) -2), b.getShortVar());

        assertEquals("B.getStringList size should match inserted triple count", 4, b.getStringList().size());

    }

    @Test
    public void testInsertQuery() throws Exception {
        A a = new A();
        URI aURI = new URI("http://test.opensilex.org/a/002");
        a.setUri(aURI);
        a.setBool(true);
        a.setCharVar('V');

        service.create(a);

        A selectedA = service.getByURI(A.class, aURI);

        assertEquals("Instance URI must be the same", aURI, selectedA.getUri());
        assertEquals("bool field values must be the same", true, selectedA.isBool());

        B b = new B();

        URI bURI = new URI("http://test.opensilex.org/b/002");

        b.setUri(bURI);
        List<String> stringList = new ArrayList<>();
        stringList.add("V1");
        stringList.add("V2");
        b.setStringList(stringList);

        service.create(b);

        B selectedB = service.getByURI(B.class, bURI);

        assertEquals("Instance URI must be the same", bURI, selectedB.getUri());
        assertEquals("B.getStringList size should match inserted triple count", stringList.size(), selectedB.getStringList().size());
    }

    @Test
    public void testDeleteQuery() throws Exception {
        A a = new A();
        URI aURI = new URI("http://test.opensilex.org/a/003");
        a.setUri(aURI);
        a.setBool(true);
        a.setCharVar('V');

        service.create(a);

        A selectedA = service.getByURI(A.class, aURI);
        assertEquals("Instance URI must be the same", aURI, selectedA.getUri());

        service.delete(A.class, aURI);
        assertNull("Object must be null after deletion", service.getByURI(A.class, aURI));
    }

    @Test
    public void testUpdateQuery() throws Exception {
        A a = new A();
        URI aURI = new URI("http://test.opensilex.org/a/004");
        a.setUri(aURI);
        a.setBool(true);
        a.setCharVar('V');
        a.setInteger(5);

        service.create(a);

        A selectedA = service.getByURI(A.class, aURI);
        assertEquals("Instance URI must be the same", aURI, selectedA.getUri());
        assertEquals("A.isBool Method should return the selected boolean", Boolean.TRUE, selectedA.isBool());
        assertEquals("A.getCharVar Method should return the selected char", 'V', (char) selectedA.getCharVar());
        assertEquals("A.getInteger Method should return the selected char", 5, (int) selectedA.getInteger());

        a.setBool(false);
        a.setCharVar('N');
        a.setInteger(null);

        service.update(a);

        A updatedA = service.getByURI(A.class, aURI);
        assertEquals("Instance URI must be the same", aURI, updatedA.getUri());
        assertEquals("A.isBool Method should return the updated boolean", Boolean.FALSE, updatedA.isBool());
        assertEquals("A.getCharVar Method should return the updated char", Character.valueOf('N'), updatedA.getCharVar());
        assertNull("A.getInteger Method should have been deleted", updatedA.getInteger());
    }

    @Test
    public void testUriExistsWithClass() throws Exception {
        URI bURI = new URI("http://test.opensilex.org/b/001");

        assertTrue("URI must exists and be of type B", service.uriExists(B.class, bURI));
    }
}

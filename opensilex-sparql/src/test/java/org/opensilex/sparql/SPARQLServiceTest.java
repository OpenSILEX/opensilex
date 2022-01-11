//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.sparql;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.opensilex.OpenSilex;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.exceptions.SPARQLInvalidUriListException;
import org.opensilex.sparql.model.*;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.unit.test.AbstractUnitTest;
import org.opensilex.uri.generation.URIGeneratorTest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;

/**
 * @author vincent
 */
public abstract class SPARQLServiceTest extends AbstractUnitTest {

    protected static SPARQLService sparql;
    protected static SPARQLModule sparqlModule;
    protected static URI ontologyDataUri;

    protected static byte[] ontologyDataBytes;
    protected static byte[] renameDataDefaultBytes;
    protected static byte[] renameDataInGraphBytes;

    public static void initialize() throws Exception {
        sparqlModule = opensilex.getModuleByClass(SPARQLModule.class);

        InputStream ontology = OpenSilex.getResourceAsStream(TEST_ONTOLOGY.FILE_PATH.toString());
        sparql.loadOntology(sparqlModule.getBaseURI(), ontology, TEST_ONTOLOGY.FILE_FORMAT);
        ontologyDataUri = sparql.getDefaultGraphURI(B.class);

        // load ontology data in memory as byte[], use it in order to load quickly (than reading file) these data during each before() method call.
        InputStream ontologyData = OpenSilex.getResourceAsStream(TEST_ONTOLOGY.DATA_FILE_PATH.toString());
        ontologyDataBytes = IOUtils.toByteArray(ontologyData);
        assert (ontologyDataBytes != null && ontologyDataBytes.length > 0);

        // load rename data in default graph
        InputStream renameDataDefault = OpenSilex.getResourceAsStream(TEST_ONTOLOGY.RENAME_DATA_DEFAULT_FILE_PATH.toString());
        renameDataDefaultBytes = IOUtils.toByteArray(renameDataDefault);
        assert (renameDataDefaultBytes != null && renameDataDefaultBytes.length > 0);

        // load rename data in named graph
        InputStream renameDataInGraph = OpenSilex.getResourceAsStream(TEST_ONTOLOGY.RENAME_DATA_IN_GRAPH_FILE_PATH.toString());
        renameDataInGraphBytes = IOUtils.toByteArray(renameDataInGraph);
        assert (renameDataInGraphBytes != null && renameDataInGraphBytes.length > 0);
    }

    @Before
    public void before() throws SPARQLException, IOException, URISyntaxException {
        // load ontology data file
        ByteArrayInputStream ontologyDataStream = new ByteArrayInputStream(ontologyDataBytes);
        sparql.loadOntology(ontologyDataUri, ontologyDataStream, TEST_ONTOLOGY.DATA_FILE_FORMAT);
        ontologyDataStream.close();

        // load test data for rename (in default graph)
        ByteArrayInputStream renameDataDefaultStream = new ByteArrayInputStream(renameDataDefaultBytes);
        sparql.loadOntology(null, renameDataDefaultStream, TEST_ONTOLOGY.RENAME_DATA_FILE_FORMAT);
        renameDataDefaultStream.close();

        // load test data for rename (in named graph)
        ByteArrayInputStream renameDataInGraphStream = new ByteArrayInputStream(renameDataInGraphBytes);
        sparql.loadOntology(new URI(TEST_ONTOLOGY.RENAME_DATA_GRAPH_URI), renameDataInGraphStream, TEST_ONTOLOGY.RENAME_DATA_FILE_FORMAT);
        renameDataInGraphStream.close();
    }

    @After
    public void after() throws Exception {
        sparql.clearGraphs(ontologyDataUri.toString(),
                TEST_ONTOLOGY.RENAME_DATA_GRAPH_URI);
    }

    @Test
    public void testAskQuery() throws SPARQLException {
        testClass(TEST_ONTOLOGY.A);
        testClass(TEST_ONTOLOGY.B);
    }

    private void testClass(Resource clazz) throws SPARQLException {
        Node owlClassNode = NodeFactory.createURI(OWL2.Class.getURI());
        AskBuilder askQuery = new AskBuilder();
        askQuery.addWhere(clazz, RDF.type, owlClassNode);
        boolean classExists = sparql.executeAskQuery(askQuery);
        assertTrue("Class " + clazz.getLocalName() + " must exists in test ontology", classExists);
    }

    @Test
    public void testGetByURI() throws Exception {
        URI aURI = new URI("http://test.opensilex.org/a/001");
        A a = sparql.getByURI(A.class, aURI, null);
        assertNotNull("http://test.opensilex.org/a/001 should exists", a);

        assertEquals("Instance URI must be the same", aURI, a.getUri());
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

        B b = a.getB();
        assertNotNull("Instance object relation should exists", b);

        URI bURI = new URI("http://test.opensilex.org/b/001");

        assertEquals("B Instance URI must be the same", bURI, b.getUri());
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

        sparql.create(a);

        A selectedA = sparql.getByURI(A.class, aURI, null);

        assertEquals("Instance URI must be the same", aURI, selectedA.getUri());
        assertEquals("bool field values must be the same", true, selectedA.isBool());

        B b = new B();

        URI bURI = new URI("http://test.opensilex.org/b/002");

        b.setUri(bURI);
        b.setFloatVar(45f);
        b.setDoubleVar(0d);
        b.setCharVar('Z');
        b.setShortVar((short) 0);
        List<String> stringList = new ArrayList<>(2);
        stringList.add("V1");
        stringList.add("V2");
        b.setStringList(stringList);

        sparql.create(b);

        B selectedB = sparql.getByURI(B.class, bURI, null);

        assertEquals("Instance URI must be the same", bURI, selectedB.getUri());
        assertEquals("B.getStringList size should match inserted triple count", stringList.size(), selectedB.getStringList().size());
    }

    @Test
    public void testCreateAll() throws Exception {

        int n = 5;

        List<A> aList = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            A a = new A();
            a.setUri(new URI("http://test.opensilex.org/a/testCreateAll" + i));
            a.setBool(true);
            a.setCharVar('V');
            aList.add(a);
        }

        sparql.create(null, aList, null, false);

        for (int i = 0; i < n; i++) {
            A createdA = aList.get(i);
            A selectedA = sparql.getByURI(A.class, createdA.getUri(), null);
            assertEquals("Instance URI must be the same", createdA.getUri(), selectedA.getUri());
        }
    }

    @Test
    public void testCreateAllWithQueryReuse() throws Exception {

        int n = 5;

        List<A> aList = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            A a = new A();
            a.setUri(new URI("http://test.opensilex.org/a/testCreateAll" + i));
            a.setBool(true);
            a.setCharVar('V');
            aList.add(a);
        }

        sparql.create(null, aList, n, false);

        for (int i = 0; i < n; i++) {
            A createdA = aList.get(i);
            A selectedA = sparql.getByURI(A.class, createdA.getUri(), null);
            assertEquals("Instance URI must be the same", createdA.getUri(), selectedA.getUri());
        }
    }

    @Test
    public void testDeleteQuery() throws Exception {
        A a = new A();
        URI aURI = new URI("http://test.opensilex.org/a/003");
        a.setUri(aURI);
        a.setBool(true);
        a.setCharVar('V');

        sparql.create(a);

        A selectedA = sparql.getByURI(A.class, aURI, null);
        assertEquals("Instance URI must be the same", aURI, selectedA.getUri());

        sparql.delete(A.class, aURI);
        assertNull("Object must be null after deletion", sparql.getByURI(A.class, aURI, null));
    }

    @Test
    public void testUpdateQuery() throws Exception {
        A a = new A();
        URI aURI = new URI("http://test.opensilex.org/a/004");
        a.setUri(aURI);
        a.setBool(true);
        a.setCharVar('V');
        a.setInteger(5);

        sparql.create(a);

        A selectedA = sparql.getByURI(A.class, aURI, null);
        assertEquals("Instance URI must be the same", aURI, selectedA.getUri());
        assertEquals("A.isBool Method should return the selected boolean", Boolean.TRUE, selectedA.isBool());
        assertEquals("A.getCharVar Method should return the selected char", 'V', (char) selectedA.getCharVar());
        assertEquals("A.getInteger Method should return the selected char", 5, (int) selectedA.getInteger());

        a.setBool(false);
        a.setCharVar('N');
        a.setInteger(null);

        sparql.update(a);

        A updatedA = sparql.getByURI(A.class, aURI, null);
        assertEquals("Instance URI must be the same", aURI, updatedA.getUri());
        assertEquals("A.isBool Method should return the updated boolean", Boolean.FALSE, updatedA.isBool());
        assertEquals("A.getCharVar Method should return the updated char", Character.valueOf('N'), updatedA.getCharVar());
        assertNull("A.getInteger Method should have been deleted", updatedA.getInteger());
    }

    @Test
    public void testUriExistsWithClass() throws Exception {
        URI bURI = new URI("http://test.opensilex.org/b/001");

        assertTrue("URI must exists and be of type B", sparql.uriExists(B.class, bURI));
    }

    @Test
    public void testUriListExistsAll() throws Exception {

        List<A> aList = new ArrayList<>();
        A a = new A();
        a.setUri(new URI("http://test.opensilex.org/a/testUriListExistsAll"));
        a.setBool(true);
        a.setCharVar('V');
        aList.add(a);

        A a2 = new A();
        a2.setUri(new URI("http://test.opensilex.org/a/testUriListExistsAll2"));
        a2.setBool(true);
        a2.setCharVar('V');
        aList.add(a2);

        sparql.create(A.class, Arrays.asList(a, a2));

        List<URI> uris = aList.stream().map(SPARQLResourceModel::getUri).collect(Collectors.toList());
        Set<URI> existingUris = sparql.getExistingUris(A.class, uris, true);

        assertEquals(existingUris.size(), uris.size());
        for (URI uri : uris) {
            URI expandedURI = new URI(SPARQLDeserializers.getExpandedURI(uri));
            assertTrue(existingUris.contains(expandedURI));
        }
    }

    @Test
    public void testUriListExistsNone() throws Exception {

        List<URI> uris = new ArrayList<>();
        uris.add(new URI("test:unknownUri1"));
        uris.add(new URI("test:unknownUri2"));

        Set<URI> existingUris = sparql.getExistingUris(A.class, uris, true);
        assertTrue(existingUris.isEmpty());

        Set<URI> unknownUris = sparql.getExistingUris(A.class, uris, false);
        assertEquals(unknownUris.size(), uris.size());
        for (URI uri : uris) {
            URI expandedURI = new URI(SPARQLDeserializers.getExpandedURI(uri));
            assertTrue(unknownUris.contains(expandedURI));
        }

    }


    @Test
    public void testRenameGraph() throws Exception {
        B b = new B();
        b.setUri(new URI("http://test.opensilex.org/a/testUriExistsWithClass"));
        b.setFloatVar(45f);
        b.setDoubleVar(0d);
        b.setCharVar('Z');
        b.setShortVar((short) 0);

        sparql.create(b);

        List<B> bList = sparql.search(B.class, null);
        assertFalse(bList.isEmpty());
        Node oldGraphNode = sparql.getDefaultGraph(B.class);
        URI newGraphUri = new URI(oldGraphNode.getURI() + "new_suffix");
        sparql.renameGraph(new URI(oldGraphNode.getURI()), newGraphUri);

        // the graph have changed so no B should be found from the old graph
        bList = sparql.search(B.class, null);
        assertTrue(bList.isEmpty());

        // the graph have changed so B should be found in the new graph
        bList = sparql.search(SPARQLDeserializers.nodeURI(newGraphUri), B.class, null, null);
        assertFalse(bList.isEmpty());
    }

    //   @Test
    public void testUriListExists() throws Exception {

        B b = new B();
        b.setFloatVar(45f);
        b.setDoubleVar(0d);
        b.setCharVar('Z');
        b.setShortVar((short) 0);

        B b1 = new B();
        b1.setFloatVar(45f);
        b1.setDoubleVar(0d);
        b1.setCharVar('Z');
        b1.setShortVar((short) 0);

        sparql.create(b);
        sparql.create(b1);

        // test one or more good URI
        assertTrue(sparql.uriListExists(B.class, Collections.singletonList(b.getUri())));
        assertTrue(sparql.uriListExists(B.class, Arrays.asList(b.getUri(), b1.getUri())));

        URI badUri = new URI(b.getUri().toString() + "prefix");
        URI badUri2 = new URI(b.getUri().toString() + "prefix2");

        // test one or more bad URI
        assertFalse(sparql.uriExists(B.class, badUri));
        assertFalse(sparql.uriListExists(B.class, Collections.singletonList(badUri)));
        assertFalse(sparql.uriListExists(B.class, Arrays.asList(badUri, badUri2)));

        // test a good and a bad URI
        assertFalse(sparql.uriListExists(B.class, Arrays.asList(b.getUri(), badUri)));
    }

    @Test
    public void testLabel() throws Exception {
        C c = new C();
        SPARQLLabel label = new SPARQLLabel("testCreateFR", "fr");
        label.addTranslation("testCreateEN", "en");
        c.setLabel(label);
        sparql.create(c);

        C cActual = sparql.getByURI(C.class, c.getUri(), "fr");
        assertEquals(c.getUri(), cActual.getUri());
        SPARQLLabel labelActual = cActual.getLabel();
        assertEquals(label.getDefaultValue(), labelActual.getDefaultValue());
        assertEquals(label.getDefaultLang(), labelActual.getDefaultLang());

        Map<String, String> others = labelActual.getTranslations();
        assertTrue(others.containsKey("en"));
        assertEquals("testCreateEN", others.get("en"));

        assertEquals(2, labelActual.getAllTranslations().size());

        cActual = sparql.getByURI(C.class, c.getUri(), "en");
        labelActual = cActual.getLabel();
        assertEquals("testCreateEN", labelActual.getDefaultValue());
        assertEquals("en", labelActual.getDefaultLang());

        others = labelActual.getTranslations();
        assertTrue(others.containsKey("fr"));
        assertEquals("testCreateFR", others.get("fr"));

        List<C> list = sparql.search(C.class, "fr");
        assertFalse(list.isEmpty());
        assertEquals(2, list.size());

        list = sparql.search(C.class, "ru");
        assertFalse(list.isEmpty());
        assertEquals(2, list.size());
        assertEquals("ru", list.get(0).getLabel().getDefaultLang());

        list = sparql.search(C.class, null);
        assertFalse(list.isEmpty());
        assertEquals(2, list.size());
        assertEquals(OpenSilex.DEFAULT_LANGUAGE, list.get(0).getLabel().getDefaultLang());

        // TODO test delete
        // TODO test update
    }

    @Test
    public void testDistinctListSearch() throws Exception {

        // this test try to insert an object b with a list and a property p,
        // b has two element in this list ( v1 and v2)
        // once this object created, we try to fetch all objects which have v1 or v2 as p value
        // if we don't use DISTINCT when selecting objects, then b appears twice in the list since it satisfy the
        // filter twice
        B b = new B();
        b.setBool(true);
        b.setFloatVar(45f);
        b.setDoubleVar(0d);
        b.setCharVar('Z');
        b.setShortVar((short) 0);

        List<String> list1 = Arrays.asList("value1", "value2");
        b.setStringList(list1);

        sparql.create(b);

        List<B> results = sparql.searchWithPagination(
                B.class,
                null,
                (SelectBuilder select) -> {
                    select.addWhere(makeVar(B.URI_FIELD), TEST_ONTOLOGY.hasStringList.asNode(), makeVar("list"));
                    SPARQLQueryHelper.addWhereValues(select, "list", b.getStringList());
                },
                null,
                0,
                10
        ).getList();

        assertEquals(1, results.size());
    }

    @Test
    @Ignore("test used to micro-bench URI generation and normalization performance")
    public void testUriGenerationPerformance() throws URISyntaxException, SPARQLException {

        int n=1000000;
        SPARQLNamedResourceModel<?>[] models = new SPARQLNamedResourceModel[n];

        String specialCharPart = RandomStringUtils.random(8,String.join("",URIGeneratorTest.EXPECTED_REMOVED_CHARACTERS));

        for (int i = 0; i < n; i++) {
            SPARQLNamedResourceModel<?> model = new SPARQLNamedResourceModel<>();
            model.setName("SPARQLNamedResourceModel"+specialCharPart+i);
            models[i] = model;
        }
        String prefix = sparql.getDefaultGenerationURI(SPARQLNamedResourceModel.class).toString();

        for(SPARQLNamedResourceModel model : models){
            URI uri = model.generateURI(prefix,model,0);
            assertNotNull(uri);
            model.setUri(uri);
        }
    }

    @Test
    public void testLoadByUris() throws Exception {

        A a1 = new A();
        A a2 = new A();
        sparql.create(a1);
        sparql.create(a2);

        List<A> loadedList = sparql.loadListByURIs(A.class,Arrays.asList(a1.getUri(),a2.getUri()),OpenSilex.DEFAULT_LANGUAGE);
        assertTrue(loadedList.contains(a1));
        assertTrue(loadedList.contains(a2));
        assertEquals(2,loadedList.size());

        // try to insert doublons, ensure loading is OK
        loadedList = sparql.loadListByURIs(A.class,Arrays.asList(a1.getUri(),a2.getUri(),a1.getUri()),OpenSilex.DEFAULT_LANGUAGE);
        assertTrue(loadedList.contains(a1));
        assertTrue(loadedList.contains(a2));
        assertEquals(2,loadedList.size());
    }

    @Test
    public void testLoadByUrisFail() throws Exception{

        A a1 = new A();
        A a2 = new A();
        sparql.create(a1);
        sparql.create(a2);

        URI unknownUri = URI.create("test:testLoadByUrisFail");

        // try to trigger exception throw
        SPARQLInvalidUriListException loadExpectedEx = assertThrows(SPARQLInvalidUriListException.class, () -> {
            sparql.loadListByURIs(A.class, Arrays.asList(a1.getUri(), a2.getUri(), unknownUri), OpenSilex.DEFAULT_LANGUAGE);
        });

        assertTrue(loadExpectedEx.getUris().contains(unknownUri));
    }

    @Test
    public void testCheckUriExists() throws Exception {
        A a = new A();
        sparql.create(a);

        boolean exists = sparql.checkTripleURIExists(a.getUri());

        assertTrue(exists);
    }

    @Test
    public void testCheckUriDoesNotExist() throws Exception {
        URI uriToCheck = new URI("http://test.opensilex.org/thisUriShouldNotExist");

        boolean exists = sparql.checkTripleURIExists(uriToCheck);

        assertFalse(exists);
    }

    @Test
    public void testRenameUri() throws Exception {
        URI uriToRename = new URI("http://test.opensilex.org/rename/uriToRename");
        URI renamedUri = new URI("http://test.opensilex.org/rename/renamedUri");
        URI a2Uri = new URI("http://test.opensilex.org/rename/a2");
        URI a3Uri = new URI("http://test.opensilex.org/rename/a3");

        A aToRename = sparql.getByURI(A.class, uriToRename, null);
        A renamedA = sparql.getByURI(A.class, renamedUri, null);
        A a2 = sparql.getByURI(A.class, a2Uri, null);
        A a3 = sparql.getByURI(A.class, a3Uri, null);

        // Verify that nothing has been renamed yet
        assertNotNull(aToRename); // subject in default graph
        assertNull(renamedA);
        assertNotNull(a2);
        assertNotNull(a3);

        assertTrue(StringUtils.isNotEmpty(a2.getPropertyToRename())); // property in default graph
        assertNull(a2.getRenamedProperty());
        assertEquals(a2.getA().getUri(), uriToRename); // object in default graph

        assertEquals(aToRename.getInteger(), Integer.valueOf(4)); // subject in named graph
        assertTrue(StringUtils.isNotEmpty(a3.getPropertyToRename())); // property in named graph
        assertNull(a3.getRenamedProperty());
        assertEquals(a3.getA().getUri(), uriToRename);

        // Execute the rename operation and update the objects
        sparql.renameTripleURI(uriToRename, renamedUri);

        aToRename = sparql.getByURI(A.class, uriToRename, null);
        renamedA = sparql.getByURI(A.class, renamedUri, null);
        a2 = sparql.getByURI(A.class, a2Uri, null);
        a3 = sparql.getByURI(A.class, a3Uri, null);

        // Assert that everything has been renamed
        assertNull(aToRename); // subject in default graph
        assertNotNull(renamedA);
        assertNotNull(a2);
        assertNotNull(a3);

        assertNull(a2.getPropertyToRename()); // property in default graph
        assertTrue(StringUtils.isNotEmpty(a2.getRenamedProperty()));
        assertEquals(a2.getA().getUri(), renamedUri); // object in default graph

        assertEquals(renamedA.getInteger(), Integer.valueOf(4)); // subject in named graph
        assertNull(a3.getPropertyToRename()); // property in named graph
        assertTrue(StringUtils.isNotEmpty(a3.getRenamedProperty()));
        assertEquals(a3.getA().getUri(), renamedUri);
    }
}

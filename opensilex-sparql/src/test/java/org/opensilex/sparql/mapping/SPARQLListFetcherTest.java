package org.opensilex.sparql.mapping;

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.A;
import org.opensilex.sparql.model.B;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.OpenSilexTestEnvironment;
import org.opensilex.utils.OrderBy;

import java.net.URI;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

/**
 * @author rcolin
 */
public class SPARQLListFetcherTest  {

    private static OpenSilexTestEnvironment openSilexTestEnv;
    private static B b1,b2,b3;

    private static B getB(){
        B b = new B();
        b.setFloatVar(45f);
        b.setDoubleVar(0d);
        b.setCharVar('Z');
        b.setShortVar((short) 0);
        return b;
    }


    @BeforeClass
    public static void beforeClass() throws Exception {

        openSilexTestEnv = OpenSilexTestEnvironment.getInstance();
        SPARQLService sparql = openSilexTestEnv.getSparql();

        // create A
        A a1 = new A();
        a1.setUri(URI.create("test:SPARQLListFetcherTest_a1"));
        A a2 = new A();
        a2.setUri(URI.create("test:SPARQLListFetcherTest_a2"));
        A a3 = new A();
        a3.setUri(URI.create("test:SPARQLListFetcherTest_a3"));

        // create B and link to A
        b1 = getB();
        b1.setUri(URI.create("test:SPARQLListFetcherTest_b1"));
        b1.setaList(Arrays.asList(a1, a2));
        b1.setStringList(Arrays.asList("opensilex","inrae","apache"));

        b2 = getB();
        b2.setUri(URI.create("test:SPARQLListFetcherTest_b2"));
        b2.setStringList(Arrays.asList("open-science","sparql","special_char;inner_separator"));

        b3 = getB();
        b3.setUri(URI.create("test:SPARQLListFetcherTest_b3"));
        b3.setaList(Arrays.asList(a1, a3));
        b3.setStringList(Arrays.asList("ontologies","reasoning","database"));

        // insert A and B into triple-store
        try {
            sparql.create(A.class, Arrays.asList(a1, a2, a3));
            sparql.create(B.class, Arrays.asList(b1,b2,b3));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void testGetList(boolean testDataProperties, boolean testObjectProperties) throws Exception {

        SPARQLService sparql = openSilexTestEnv.getSparql();

        List<OrderBy> orderByList = Collections.singletonList(SPARQLClassObjectMapper.DEFAULT_ORDER_BY);
        AtomicReference<SelectBuilder> initialSelect = new AtomicReference<>();

        List<B> initialResults = sparql.search(B.class, null, initialSelect::set, orderByList);

        Map<String,Boolean> fieldsToFetch = new HashMap<>();
        if(testDataProperties){
            fieldsToFetch.put(B.STRING_LIST_FIELD,true);
        }
        if(testObjectProperties){
            fieldsToFetch.put(B.A_LIST_FIELD,true);
        }

        SPARQLListFetcher<B> listFetcher = new SPARQLListFetcher<>(
                sparql,
                B.class,
                null,
                fieldsToFetch,
                initialSelect.get(),
                initialResults
        );
        listFetcher.updateModels();

        assertModelEquals(b1,initialResults.get(0));
        assertModelEquals(b2,initialResults.get(1));
        assertModelEquals(b3,initialResults.get(2));
    }

    private void assertModelEquals(B expected, B actual){
        assertTrue(SPARQLDeserializers.compareURIs(expected.getUri(),actual.getUri()));

        if(expected.getaList() != null){
            assertNotNull(actual.getaList());
            assertEquals(expected.getaList().size(),actual.getaList().size());
            assertTrue(expected.getaList().containsAll(actual.getaList()));
        }

        if(expected.getStringList() != null){
            assertNotNull(actual.getStringList());
            assertEquals(expected.getStringList().size(),actual.getStringList().size());
            assertTrue(expected.getStringList().containsAll(actual.getStringList()));
        }
    }

    @Test
    public void testGetObjectList() throws Exception {
        testGetList(false,true);
    }

    @Test
    public void testGetDataList() throws Exception {
        testGetList(true,false);
    }

    @Test
    public void testGetDataAndObjectList() throws Exception {
        testGetList(true,true);
    }

    @Test
    public void testFailWithNoProperties() throws Exception {
        SPARQLService sparql = openSilexTestEnv.getSparql();

        List<OrderBy> orderByList = Collections.singletonList(SPARQLClassObjectMapper.DEFAULT_ORDER_BY);

        AtomicReference<SelectBuilder> initialSelectRef = new AtomicReference<>();
        List<B> initialResults = sparql.search(B.class, null, initialSelectRef::set, orderByList);
        SelectBuilder initialSelect = initialSelectRef.get();

        // no properties test
        Map<String,Boolean> fieldsToFetch = new HashMap<>();

        assertThrows(IllegalArgumentException.class,() -> new SPARQLListFetcher<>(
                sparql,
                B.class,
                null,
                fieldsToFetch,
                initialSelect,
                initialResults
        ));
    }

    @Test
    public void testFailWithUnknownProperties() throws Exception {
        SPARQLService sparql = openSilexTestEnv.getSparql();

        List<OrderBy> orderByList = Collections.singletonList(SPARQLClassObjectMapper.DEFAULT_ORDER_BY);

        AtomicReference<SelectBuilder> initialSelectRef = new AtomicReference<>();
        List<B> initialResults = sparql.search(B.class, null, initialSelectRef::set, orderByList);
        SelectBuilder initialSelect = initialSelectRef.get();

        // unknown property test
        Map<String,Boolean> fieldsToFetch = new HashMap<>();
        fieldsToFetch.put("unknown_object_property",true);

        assertThrows(IllegalArgumentException.class,() -> new SPARQLListFetcher<>(
                sparql,
                B.class,
                null,
                fieldsToFetch,
                initialSelect,
                initialResults
        ));

    }

}
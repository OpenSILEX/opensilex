package org.opensilex.sparql.mapping;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.jena.arq.querybuilder.Order;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensilex.sparql.deserializer.SPARQLDeserializerNotFoundException;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.exceptions.SPARQLInvalidClassDefinitionException;
import org.opensilex.sparql.exceptions.SPARQLMapperNotFoundException;
import org.opensilex.sparql.model.A;
import org.opensilex.sparql.model.B;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.OpenSilexTestEnvironment;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;

import java.net.URI;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * @author rcolin
 */
public class SPARQLListFetcherTest {

    private static OpenSilexTestEnvironment openSilexTestEnv;
    private static B b1, b2, b3;

    private static B getB() {
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
        b1.setStringList(Arrays.asList("opensilex", "inrae", "apache"));

        b2 = getB();
        b2.setUri(URI.create("test:SPARQLListFetcherTest_b2"));
        b2.setStringList(Arrays.asList("open-science", "sparql", "special_char;inner_separator"));

        b3 = getB();
        b3.setUri(URI.create("test:SPARQLListFetcherTest_b3"));
        b3.setaList(Arrays.asList(a1, a3));
        b3.setStringList(Arrays.asList("ontologies", "reasoning", "database"));

        // insert A and B into triple-store
        try {
            sparql.create(A.class, Arrays.asList(a1, a2, a3));
            sparql.create(B.class, Arrays.asList(b1, b2, b3));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void testGetList(boolean testDataProperties, boolean testObjectProperties) throws Exception {

        SPARQLService sparql = openSilexTestEnv.getSparql();

        List<OrderBy> orderByList = Collections.singletonList(SPARQLClassObjectMapper.DEFAULT_ORDER_BY);
        AtomicReference<SelectBuilder> initialSelect = new AtomicReference<>();

        List<B> initialResults = sparql.search(B.class, null, initialSelect::set, orderByList);

        Set<String> fieldsToFetch = new HashSet<>();
        if (testDataProperties) {
            fieldsToFetch.add(B.STRING_LIST_FIELD);
        }
        if (testObjectProperties) {
            fieldsToFetch.add(B.A_LIST_FIELD);
        }

        SPARQLListFetcher<B> listFetcher = new SPARQLListFetcher<>(
                sparql,
                B.class,
                null,
                fieldsToFetch,
                initialResults
        );
        listFetcher.updateModels();

        assertModelEquals(b1, initialResults.get(0));
        assertModelEquals(b2, initialResults.get(1));
        assertModelEquals(b3, initialResults.get(2));
    }

    private void assertModelEquals(B expected, B actual) {
        assertTrue(SPARQLDeserializers.compareURIs(expected.getUri(), actual.getUri()));

        if (expected.getaList() != null) {
            assertNotNull(actual.getaList());
            assertEquals(expected.getaList().size(), actual.getaList().size());
            assertTrue(expected.getaList().containsAll(actual.getaList()));
        }

        if (expected.getStringList() != null) {
            assertNotNull(actual.getStringList());
            assertEquals(expected.getStringList().size(), actual.getStringList().size());
            assertTrue(expected.getStringList().containsAll(actual.getStringList()));
        }
    }

    @Test
    public void testGetObjectList() throws Exception {
        testGetList(false, true);
    }

    @Test
    public void testGetDataList() throws Exception {
        testGetList(true, false);
    }

    @Test
    public void testGetDataAndObjectList() throws Exception {
        testGetList(true, true);
    }

    @Test
    public void testFailWithNoProperties() throws Exception {
        SPARQLService sparql = openSilexTestEnv.getSparql();

        List<OrderBy> orderByList = Collections.singletonList(SPARQLClassObjectMapper.DEFAULT_ORDER_BY);

        AtomicReference<SelectBuilder> initialSelectRef = new AtomicReference<>();
        List<B> initialResults = sparql.search(B.class, null, initialSelectRef::set, orderByList);

        assertThrows(IllegalArgumentException.class, () -> new SPARQLListFetcher<>(
                sparql,
                B.class,
                null,
                Collections.emptyList(),
                initialResults
        ));
    }

    @Test
    public void testFailWithUnknownProperties() throws Exception {
        SPARQLService sparql = openSilexTestEnv.getSparql();

        List<OrderBy> orderByList = Collections.singletonList(SPARQLClassObjectMapper.DEFAULT_ORDER_BY);

        AtomicReference<SelectBuilder> initialSelectRef = new AtomicReference<>();
        List<B> initialResults = sparql.search(B.class, null, initialSelectRef::set, orderByList);

        // unknown property test
        Set<String> fieldsToFetch = new HashSet<>();
        fieldsToFetch.add("unknown_object_property");

        assertThrows(IllegalArgumentException.class, () -> new SPARQLListFetcher<>(
                sparql,
                B.class,
                null,
                fieldsToFetch,
                initialResults
        ));
    }

    @Test
    public void testFailWithDuplicateResults() throws Exception {

        SPARQLService sparql = openSilexTestEnv.getSparql();

        List<OrderBy> orderByList = Collections.singletonList(SPARQLClassObjectMapper.DEFAULT_ORDER_BY);

        AtomicReference<SelectBuilder> initialSelectRef = new AtomicReference<>();
        List<B> initialResults = sparql.search(B.class, null, initialSelectRef::set, orderByList);

        // append a duplicate into models to ensure fail
        initialResults.add(initialResults.get(0));

        // unknown property test
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            SPARQLListFetcher<B> listFetcher = new SPARQLListFetcher<>(
                    sparql,
                    B.class,
                    null,
                    Collections.singleton(B.A_LIST_FIELD),
                    initialResults
            );
            listFetcher.updateModels();
        });

        Assert.assertTrue(ex.getMessage().startsWith("Multiple results with the same URI"));
    }


    /**
     * Test that {@link SPARQLListFetcher} properly use URIS provided into VALUES clause by the initialSelect
     */
    @Test
    public void testWithInitialSelectWithValuesHandler() throws Exception {

        SPARQLService sparql = openSilexTestEnv.getSparql();

        List<OrderBy> orderByList = Collections.singletonList(SPARQLClassObjectMapper.DEFAULT_ORDER_BY);


        Object[] uriNodes = SPARQLDeserializers.nodeListURIAsArray(Arrays.asList(b1.getUri(), b3.getUri()));

        // only fetch b1 and b3
        List<B> initialResults = sparql.search(B.class, null, (select) -> {

            // test with addValueVar
            select.addValueVar(SPARQLResourceModel.URI_FIELD, uriNodes);
        }, orderByList);

        assertEquals(2, initialResults.size());

        Set<String> fieldsToFetch = new HashSet<>();
        fieldsToFetch.add(B.STRING_LIST_FIELD);
        fieldsToFetch.add(B.A_LIST_FIELD);

        SPARQLListFetcher<B> listFetcher = new SPARQLListFetcher<>(
                sparql,
                B.class,
                null,
                fieldsToFetch,
                initialResults
        );
        listFetcher.updateModels();

        assertModelEquals(b1, initialResults.get(0));
        assertModelEquals(b3, initialResults.get(1));
    }

    /**
     * Test that {@link SPARQLListFetcher} properly use URIS provided into VALUES clause by the initialSelect.
     */
    @Test
    public void testWithInitialSelectWithWhereValuesHandler() throws Exception {

        SPARQLService sparql = openSilexTestEnv.getSparql();

        List<OrderBy> orderByList = Collections.singletonList(SPARQLClassObjectMapper.DEFAULT_ORDER_BY);

        Object[] uriNodes = SPARQLDeserializers.nodeListURIAsArray(Arrays.asList(b1.getUri(), b3.getUri()));

        // only fetch b1 and b3
        List<B> initialResults = sparql.search(B.class, null, (select) -> {

            // test with addWhereValueVar
            select.addWhereValueVar(SPARQLResourceModel.URI_FIELD, uriNodes);
        }, orderByList);

        assertEquals(2, initialResults.size());

        Set<String> fieldsToFetch = new HashSet<>();
        fieldsToFetch.add(B.STRING_LIST_FIELD);
        fieldsToFetch.add(B.A_LIST_FIELD);

        SPARQLListFetcher<B> listFetcher = new SPARQLListFetcher<>(
                sparql,
                B.class,
                null,
                fieldsToFetch,
                initialResults
        );
        listFetcher.updateModels();

        assertModelEquals(b1, initialResults.get(0));
        assertModelEquals(b3, initialResults.get(1));
    }

    private void testSearchWithPaginationAndOrder(SPARQLService sparql, Collection<String> orders, int page, int pageSize) throws Exception {
        // search
        ListWithPagination<B> paginatedResults = sparql.searchWithPagination(
                B.class,
                null,
                (select) -> {
                    select.addFilter(SPARQLQueryHelper.regexFilter(B.STRING_FIELD, "testSearchWithAdvancedPagination"));
                },
                orders.stream().map(orderStr -> new OrderBy(orderStr, Order.ASCENDING)).collect(Collectors.toList()),
                page,
                pageSize
        );
        List<B> resultsFromDB = paginatedResults.getList();
        assertEquals(pageSize, resultsFromDB.size());

        // multivalued properties fetching
        SPARQLListFetcher<B> listFetcher = new SPARQLListFetcher<>(
                sparql,
                B.class,
                null,
                Collections.singleton(B.STRING_LIST_FIELD),
                resultsFromDB
        );
        listFetcher.updateModels();

        // check that multivalued field have been updated and with the good element
        for (B result : resultsFromDB) {
            Assert.assertFalse(CollectionUtils.isEmpty(result.getStringList()));
            String modelIdx = result.getUri().toString().split("_")[1];
            Assert.assertTrue(result.getStringList().contains("string_a_" + modelIdx));
            Assert.assertTrue(result.getStringList().contains("string_b_" + modelIdx));
        }
    }

    @Test
    public void testSearchWithAdvancedPagination() throws Exception {

        SPARQLService sparql = openSilexTestEnv.getSparql();
        int nbModel = 100;
        int pageSize = 10;

        // use a property in order to discriminate the models created here, from global B models
        String stringVar = "testSearchWithAdvancedPagination";

        // test to create a list of models in order to check pagination
        List<B> models = new ArrayList<>(nbModel);
        for (int i = 0; i < nbModel; i++) {
            B model = getB();
            model.setUri(URI.create("test:testSearchWithAdvancedPagination_"+i));
            model.setStringList(Arrays.asList("string_a_" + i, "string_b_" + i));
            model.setStringVar(stringVar);
            models.add(model);
        }
        sparql.create(B.class, models);


        // paginated search lookup
        for (int i = 0; i < nbModel / pageSize; i++) {

            // test with several orders
            testSearchWithPaginationAndOrder(sparql, Collections.emptyList(), i, pageSize);
            testSearchWithPaginationAndOrder(sparql, Collections.singletonList(SPARQLResourceModel.URI_FIELD), i, pageSize);
            testSearchWithPaginationAndOrder(sparql, Arrays.asList(B.STRING_FIELD,B.DOUBLE_FIELD), i, pageSize);
            testSearchWithPaginationAndOrder(sparql, Arrays.asList(SPARQLResourceModel.URI_FIELD, B.STRING_FIELD,B.DOUBLE_FIELD), i, pageSize);
        }
    }

}
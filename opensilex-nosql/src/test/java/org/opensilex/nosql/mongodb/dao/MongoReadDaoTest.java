package org.opensilex.nosql.mongodb.dao;

import com.mongodb.client.model.Projections;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.*;
import org.opensilex.nosql.MongoDBServiceTest;
import org.opensilex.nosql.exceptions.NoSQLInvalidURIException;
import org.opensilex.nosql.mongodb.model.MongoTestModel;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.pagination.StreamWithPagination;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import static org.junit.Assert.*;

public class MongoReadDaoTest extends MongoDBServiceTest {

    protected static MongoReadWriteDao<MongoTestModel, MongoSearchFilter> dao;
    private static final String TEST_DATASET_BASE_TYPE_URI = "opensilex:type_:";
    private static final List<URI> TYPE_LIST = List.of(
            URI.create(TEST_DATASET_BASE_TYPE_URI+1),
            URI.create(TEST_DATASET_BASE_TYPE_URI+2),
            URI.create(TEST_DATASET_BASE_TYPE_URI+3)
    );

    private static final String TEST_DATASET_BASE_URI = "opensilex:";
    private static final URI SINGLETON_URI = URI.create(TEST_DATASET_BASE_URI+1);
    private static final List<URI> URI_LIST = List.of(
            SINGLETON_URI,
            URI.create(TEST_DATASET_BASE_URI+2),
            URI.create(TEST_DATASET_BASE_URI+3)
    );
    private static final int EXPECTED_RESULT_BY_TYPE = 10;

    private static final Bson PROJECTION = Projections.fields(
            Projections.include(MongoTestModel.URI_FIELD, MongoTestModel.TYPE_FIELD, MongoTestModel.NAME_FIELD, MongoTestModel.ID_FIELD),
            Projections.exclude(MongoTestModel.TAGS_FIELD, MongoTestModel.VALUES_FIELD)
    );

    @BeforeClass
    public static void setUp() {
        MongoDBServiceTest.setUp();
        dao = new MongoReadWriteDao<>(getNewMongoDBService(), MongoTestModel.class, "mongo-dao-test", "test");
        // Load data from json file
    }

    @AfterClass
    public static void stop() {
        MongoDBServiceTest.stop();
    }

    @Test
    public void get() throws NoSQLInvalidURIException {
        assertNotNull(dao.get(SINGLETON_URI));
        Assert.assertThrows(IllegalArgumentException.class, () -> dao.get(null));
        Assert.assertThrows(NoSQLInvalidURIException.class, () -> dao.get(URI.create(":fake_uri")));
    }

    @Test
    public void getWithSession() {

        getNewMongoDBService().runTransaction((session) -> {
            // Insert model
            URI uri = null;
            assertNotNull(dao.get(session, uri));

            // Should not exist outside of session since, transaction has not been committed
            Assert.assertThrows(NoSQLInvalidURIException.class, () -> dao.get(uri));
        });
    }

    @Test
    public void exists() {
        assertTrue(dao.exists(SINGLETON_URI));
        Assert.assertThrows(IllegalArgumentException.class, () -> dao.exists(null));
        assertFalse(dao.exists(URI.create(":fake_uri")));
    }

    @Test
    public void existsWithSession() {

        getNewMongoDBService().runTransaction((session) -> {
            URI uri = null;
            // Insert model
            assertTrue(dao.exists(session, uri));

            // Should not exist outside of session since, transaction has not been committed
            assertFalse(dao.exists(uri));
        });
    }

    @Test
    public void count() {

        MongoSearchFilter filter = new MongoSearchFilter().setUri(SINGLETON_URI);
        assertEquals(1, dao.count(filter));

        filter = new MongoSearchFilter();
        filter.setRdfTypes(TYPE_LIST);
        assertEquals(TYPE_LIST.size() * EXPECTED_RESULT_BY_TYPE, dao.count(filter));

        filter = new MongoSearchFilter();
        filter.setIncludedUris(URI_LIST);
        assertEquals(URI_LIST.size(), dao.count(filter));
    }

    @Test
    public void countWithSession() {
    }

    @Test
    public void search() {
        MongoSearchFilter filter = new MongoSearchFilter().setUri(SINGLETON_URI);
        ListWithPagination<MongoTestModel> results = dao.search(filter);
        assertNotNull(results.getList());
        assertEquals(1, results.getList().size());

        filter = new MongoSearchFilter();
        filter.setRdfTypes(TYPE_LIST);
        results = dao.search(filter);
        assertNotNull(results.getList());
        assertEquals(TYPE_LIST.size() * EXPECTED_RESULT_BY_TYPE, results.getList().size());

        filter = new MongoSearchFilter();
        filter.setIncludedUris(URI_LIST);
        results = dao.search(filter);
        assertNotNull(results);
        assertNotNull(results.getList());
        assertEquals(URI_LIST.size(), results.getList().size());
    }

    @Test
    public void searchWithSession() {
    }


    protected void testFieldProjection(ListWithPagination<MongoTestModel> results) {
        for (MongoTestModel modelFromDB : results.getList()) {
            assertNotNull(modelFromDB.getUri());
            assertNotNull(modelFromDB.getRdfType());
            assertNotNull(modelFromDB.getName());
            assertNotNull(modelFromDB.getId());
            assertNull(modelFromDB.getTags());
            assertNull(modelFromDB.getValues());
        }
    }

    @Test
    public void searchWithProjection() {

        MongoSearchFilter filter = new MongoSearchFilter().setUri(SINGLETON_URI);
        ListWithPagination<MongoTestModel> results = dao.search(null, filter, PROJECTION);
        assertNotNull(results.getList());
        assertEquals(1, results.getList().size());
        testFieldProjection(results);

        filter = new MongoSearchFilter();
        filter.setRdfTypes(TYPE_LIST);
        results = dao.search(null, filter, PROJECTION);
        assertNotNull(results.getList());
        assertEquals(TYPE_LIST.size() * EXPECTED_RESULT_BY_TYPE, results.getList().size());
        testFieldProjection(results);

        filter = new MongoSearchFilter();
        filter.setIncludedUris(URI_LIST);
        results = dao.search(null, filter, PROJECTION);
        assertNotNull(results.getList());
        assertEquals(URI_LIST.size(), results.getList().size());
        testFieldProjection(results);
    }

    protected void testDocumentProjection(ListWithPagination<Document> results) {
        for (Document document : results.getList()) {
            assertNotNull(document.get("rdfType"));
            assertNotNull(document.get("uri"));
            assertNotNull(document.get("name"));
            assertNotNull(document.get("id"));
            assertNull(document.get("values"));
            assertNull(document.get("tags"));
        }
    }


    @Test
    public void searchWithConversion() {

        Function<MongoTestModel, Document> convertor = (model) -> {
            Document doc = new Document();
            doc.put("rdfType", model.getRdfType());
            doc.put("uri", model.getUri());
            doc.put("name", model.getName());
            doc.put("id", model.getId());
            doc.put("values", model.getValues());
            doc.put("tags", model.getTags());
            return doc;
        };

        MongoSearchFilter filter = new MongoSearchFilter().setUri(SINGLETON_URI);
        ListWithPagination<Document> results = dao.search(null,filter, PROJECTION, convertor);
        assertNotNull(results.getList());
        assertEquals(1, results.getList().size());
        testDocumentProjection(results);

        filter = new MongoSearchFilter();
        filter.setRdfTypes(TYPE_LIST);
        results = dao.search(null,filter, PROJECTION, convertor);
        assertNotNull(results.getList());
        assertEquals(TYPE_LIST.size() * EXPECTED_RESULT_BY_TYPE, results.getList().size());
        testDocumentProjection(results);

        filter = new MongoSearchFilter();
        filter.setIncludedUris(URI_LIST);
        results = dao.search(null,filter, PROJECTION, convertor);
        assertNotNull(results.getList());
        assertEquals(URI_LIST.size(), results.getList().size());
        testDocumentProjection(results);
    }

    @Test
    public void searchAsStream() {
        MongoSearchFilter filter = new MongoSearchFilter().setUri(SINGLETON_URI);
        StreamWithPagination<MongoTestModel> results = dao.searchAsStream(filter);
        assertNotNull(results);
        assertNotNull(results.getStream());
        assertEquals(1, results.getTotal());

        filter = new MongoSearchFilter();
        filter.setRdfTypes(TYPE_LIST);
        results = dao.searchAsStream(filter);
        assertNotNull(results);
        assertNotNull(results.getStream());
        assertEquals(TYPE_LIST.size() * EXPECTED_RESULT_BY_TYPE, results.getTotal());

        filter = new MongoSearchFilter();
        filter.setIncludedUris(URI_LIST);
        results = dao.searchAsStream(filter);
        assertNotNull(results);
        assertNotNull(results.getStream());
        assertEquals(URI_LIST.size(), results.getTotal());
    }

    @Test
    public void searchAsStreamWithSession() {
    }

    @Test
    public void searchAsStreamWithProjection() {
    }

//    @Test
//    public void distinctUris() {
//    }
//
//    @Test
//    public void distinctUrisWithSession() {
//    }
//
//    @Test
//    public void distinct() {
//    }
//
//    @Test
//    public void distinctAggregation() {
//    }
//
//    @Test
//    public void lookupAggregation() {
//    }
}
package org.opensilex.nosql.mongodb.dao;

import com.mongodb.client.model.Projections;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.*;
import org.opensilex.nosql.MongoDBServiceTest;
import org.opensilex.nosql.exceptions.NoSQLInvalidURIException;
import org.opensilex.nosql.mongodb.model.MongoTestModel;
import org.opensilex.utils.ListWithPagination;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.function.Function;

import static org.junit.Assert.*;

public class MongoReadDaoTest extends MongoDBServiceTest {

    protected static MongoReadWriteDao<MongoTestModel, MongoSearchFilter> dao;

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
        URI uri = null;
        assertNotNull(dao.get(uri));
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
        assertTrue(dao.exists(URI.create("")));
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

        URI uri = null;
        MongoSearchFilter uriFilter = new MongoSearchFilter().setUri(uri);
        assertEquals(1, dao.count(uriFilter));

        MongoSearchFilter typeFilter = new MongoSearchFilter();
        typeFilter.setRdfTypes(Collections.singletonList(URI.create("vocabulary:data")));
        assertEquals(100, dao.count(typeFilter));

        URI uri2 = null, uri3 = null;
        MongoSearchFilter urisFilter = new MongoSearchFilter();
        typeFilter.setIncludedUris(Arrays.asList(uri, uri2, uri3));
        assertEquals(3, dao.count(urisFilter));
    }

    @Test
    public void countWithSession() {
    }

    @Test
    public void search() {
        URI uri = null;
        MongoSearchFilter filter = new MongoSearchFilter().setUri(uri);
        ListWithPagination<MongoTestModel> results = dao.search(filter);
        assertNotNull(results.getList());
        assertEquals(1, results.getList().size());

        filter = new MongoSearchFilter();
        filter.setRdfTypes(Collections.singletonList(URI.create("vocabulary:data")));

        results = dao.search(filter);
        assertNotNull(results.getList());
        assertEquals(100, results.getList().size());

        URI uri2 = null, uri3 = null;
        filter.setIncludedUris(Arrays.asList(uri, uri2, uri3));

        results = dao.search(filter);
        assertNotNull(results);
        assertNotNull(results.getList());
        assertEquals(100, results.getList().size());
    }

    @Test
    public void searchWithSession() {
    }


    protected void testFieldProjection(ListWithPagination<MongoTestModel> results) {
        for (MongoTestModel modelFromDB : results.getList()) {
            assertNotNull(modelFromDB.getUri());
            assertNotNull(modelFromDB.getRdfType());
            assertNotNull(modelFromDB.getName());
            assertNull(modelFromDB.getTimestamp());
            assertNull(modelFromDB.getId());
        }
    }

    @Test
    public void searchWithProjection() {

        Bson projection = Projections.fields(
                Projections.include(MongoTestModel.URI_FIELD, MongoTestModel.TYPE_FIELD, MongoTestModel.NAME_FIELD),
                Projections.exclude(MongoTestModel.TIMESTAMP_FIELD, MongoTestModel.ID_FIELD)
        );

        URI uri = null;
        MongoSearchFilter filter = new MongoSearchFilter().setUri(uri);
        ListWithPagination<MongoTestModel> results = dao.search(null, filter, projection);
        assertNotNull(results.getList());
        assertEquals(1, results.getList().size());
        testFieldProjection(results);

        filter = new MongoSearchFilter();
        filter.setRdfTypes(Collections.singletonList(URI.create("vocabulary:data")));

        results = dao.search(null, filter, projection);
        assertNotNull(results.getList());
        assertEquals(100, results.getList().size());
        testFieldProjection(results);

        URI uri2 = null, uri3 = null;
        filter.setIncludedUris(Arrays.asList(uri, uri2, uri3));

        results = dao.search(null, filter, projection);
        assertNotNull(results.getList());
        assertEquals(100, results.getList().size());
        testFieldProjection(results);
    }

    protected void testDocumentProjection(ListWithPagination<Document> results) {
        for (Document document : results.getList()) {
            assertNotNull(document.get("rdfType"));
            assertNotNull(document.get("uri"));
            assertNotNull(document.get("name"));
            assertNull(document.get("timestamp"));
            assertNull(document.get("id"));
        }
    }


    @Test
    public void searchWithConversion() {

        Bson projection = Projections.fields(
                Projections.include(MongoTestModel.URI_FIELD, MongoTestModel.TYPE_FIELD, MongoTestModel.NAME_FIELD),
                Projections.exclude(MongoTestModel.TIMESTAMP_FIELD, MongoTestModel.ID_FIELD)
        );

        Function<MongoTestModel, Document> convertor = (model) -> {
            Document doc = new Document();
            doc.put("rdfType", model.getRdfType());
            doc.put("uri", model.getUri());
            doc.put("name", model.getName());
            doc.put("timestamp", model.getTimestamp());
            doc.put("id", model.getId());
            return doc;
        };

        URI uri = null;
        MongoSearchFilter filter = new MongoSearchFilter().setUri(uri);
        ListWithPagination<Document> results = dao.search(null,filter, projection, convertor);
        assertNotNull(results.getList());
        assertEquals(1, results.getList().size());
        testDocumentProjection(results);

        filter = new MongoSearchFilter();
        filter.setRdfTypes(Collections.singletonList(URI.create("vocabulary:data")));

        results = dao.search(null,filter, projection, convertor);
        assertNotNull(results.getList());
        assertEquals(100, results.getList().size());
        testDocumentProjection(results);

        URI uri2 = null, uri3 = null;
        filter.setIncludedUris(Arrays.asList(uri, uri2, uri3));

        results = dao.search(null,filter, projection, convertor);
        assertNotNull(results.getList());
        assertEquals(100, results.getList().size());
        testDocumentProjection(results);
    }

    @Test
    public void searchAsStream() {
    }

    @Test
    public void searchAsStreamWithSession() {
    }

    @Test
    public void searchAsStreamWithProjection() {
    }

    @Test
    public void distinctUris() {
    }

    @Test
    public void distinctUrisWithSession() {
    }

    @Test
    public void distinct() {
    }

    @Test
    public void distinctAggregation() {
    }

    @Test
    public void lookupAggregation() {
    }
}
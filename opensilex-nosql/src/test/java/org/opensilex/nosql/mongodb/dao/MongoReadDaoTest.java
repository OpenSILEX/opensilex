package org.opensilex.nosql.mongodb.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mongodb.client.model.Projections;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.*;
import org.opensilex.nosql.MongoDBServiceTest;
import org.opensilex.nosql.exceptions.NoSQLInvalidURIException;
import org.opensilex.nosql.mongodb.model.MongoTestModel;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.pagination.StreamWithPagination;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.Assert.*;

public class MongoReadDaoTest extends MongoDBServiceTest {

    protected static MongoReadWriteDao<MongoTestModel, MongoSearchFilter> dao;

    private static final String TEST_DATASET_BASE_TYPE_URI = "opensilex:type_";
    private static final List<URI> TYPE_LIST = List.of(
            URI.create(TEST_DATASET_BASE_TYPE_URI + 1),
            URI.create(TEST_DATASET_BASE_TYPE_URI + 2),
            URI.create(TEST_DATASET_BASE_TYPE_URI + 3)
    );

    private static final String TEST_DATASET_BASE_URI = "opensilex:";
    private static final URI SINGLETON_URI = URI.create(TEST_DATASET_BASE_URI + 1);
    private static final List<URI> URI_LIST = List.of(
            SINGLETON_URI,
            URI.create(TEST_DATASET_BASE_URI + 2),
            URI.create(TEST_DATASET_BASE_URI + 3)
    );
    private static final int EXPECTED_RESULT = 100;
    private static final int EXPECTED_RESULT_BY_TYPE = 10;
    private static final Bson PROJECTION = Projections.fields(
            Projections.include(MongoTestModel.URI_FIELD, MongoTestModel.TYPE_FIELD, MongoTestModel.NAME_FIELD, MongoTestModel.ID_FIELD)
    );

    private static final Path JSON_FILE_PATH = Paths.get("src", "test", "resources", "generated_documents.zip");

    @BeforeClass
    public static void setUp() {
        MongoDBServiceTest.setUp();
        dao = new MongoReadWriteDao<>(mongoDBServiceV2, MongoTestModel.class, "mongo-dao-test", "test");
        LOGGER.debug("Load json dump for testing : {} [START]", JSON_FILE_PATH);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        TypeFactory typeFactory = objectMapper.getTypeFactory();
        CollectionType collectionType = typeFactory.constructCollectionType(List.class, MongoTestModel.class);

        // Extract .json ZipEntry from file, read it and convert to JSON document
        Instant start = Instant.now();
        try (ZipFile zipFile = new ZipFile(JSON_FILE_PATH.toFile())) {
            ZipEntry entry = zipFile.getEntry("generated_documents.json");
            InputStream stream = zipFile.getInputStream(entry);
            List<MongoTestModel> models = objectMapper.readValue(stream.readAllBytes(), collectionType);

            // Insert documents
            mongoDBServiceV2.runTransaction(session -> dao.getCollection().insertMany(session, models));

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // Load data from json file
        long durationMs = Duration.between(start, Instant.now()).toMillis();
        LOGGER.debug("Load json dump for testing : {} [OK] duration: {} ms", JSON_FILE_PATH, durationMs);

    }

    @AfterClass
    public static void stop() {
        MongoDBServiceTest.stop();
    }

    @Test
    public void get() throws NoSQLInvalidURIException {
        assertNotNull(dao.get(SINGLETON_URI));
        Assert.assertThrows(NullPointerException.class, () -> dao.get(null));
        Assert.assertThrows(NoSQLInvalidURIException.class, () -> dao.get(URI.create("opensilex:fake_uri")));
    }

    @Test
    public void getWithSession() {

//        mongoDBServiceV2.runTransaction((session) -> {
//            // Insert model
//            URI uri = null;
//            assertNotNull(dao.get(session, uri));
//
//            // Should not exist outside of session since, transaction has not been committed
//            Assert.assertThrows(NoSQLInvalidURIException.class, () -> dao.get(uri));
//        });
    }

    @Test
    public void exists() {
        assertTrue(dao.exists(SINGLETON_URI));
        Assert.assertThrows(NullPointerException.class, () -> dao.exists(null));
        assertFalse(dao.exists(URI.create("opensilex:fake_uri")));
    }

    @Test
    public void existsWithSession() {

//        mongoDBServiceV2.runTransaction((session) -> {
//            URI uri = null;
//            // Insert model
//            assertTrue(dao.exists(session, uri));
//
//            // Should not exist outside of session since, transaction has not been committed
//            assertFalse(dao.exists(uri));
//        });
    }

    @Test
    public void count() {

        MongoSearchFilter filter = new MongoSearchFilter().setUri(SINGLETON_URI);
        assertEquals(1, dao.count(filter));

        filter = new MongoSearchFilter();
        assertEquals(EXPECTED_RESULT, dao.count(filter));

        filter.setRdfTypes(TYPE_LIST);
        assertEquals(TYPE_LIST.size() * EXPECTED_RESULT_BY_TYPE, dao.count(filter));

        filter = new MongoSearchFilter();
        filter.setIncludedUris(URI_LIST);
        assertEquals(URI_LIST.size(), dao.count(filter));
    }

//    @Test
//    public void countWithSession() {
//    }

    @Test
    public void search() {
        MongoSearchFilter filter = new MongoSearchFilter().setUri(SINGLETON_URI);
        ListWithPagination<MongoTestModel> results = dao.search(filter);
        assertNotNull(results.getList());
        assertEquals(1, results.getList().size());


        filter = new MongoSearchFilter();
        results = dao.search(filter);
        assertEquals(EXPECTED_RESULT, results.getList().size());

        filter.setRdfTypes(TYPE_LIST);
        results = dao.search(filter);
        assertNotNull(results.getList());
        assertEquals(TYPE_LIST.size() * EXPECTED_RESULT_BY_TYPE, results.getList().size());

        filter = new MongoSearchFilter();
        filter.setIncludedUris(URI_LIST);
        results = dao.search(filter);
        assertNotNull(results.getList());
        assertEquals(URI_LIST.size(), results.getList().size());
    }

    @Test
    public void searchWithSession() {
    }

    protected void testFieldProjection(MongoTestModel model) {
        assertNotNull(model.getUri());
        assertNotNull(model.getRdfType());
        assertNotNull(model.getName());
        assertNotNull(model.getId());
        assertNull(model.getTags());
        assertNull(model.getValues());
    }

    protected void testFieldProjection(ListWithPagination<MongoTestModel> results) {
        results.getList().forEach(this::testFieldProjection);
    }

    protected void testFieldProjection(StreamWithPagination<MongoTestModel> results) {
        results.getStream().forEach(this::testFieldProjection);
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
        ListWithPagination<Document> results = dao.search(null, filter, PROJECTION, convertor);
        assertNotNull(results.getList());
        assertEquals(1, results.getList().size());
        testDocumentProjection(results);

        filter = new MongoSearchFilter();
        filter.setRdfTypes(TYPE_LIST);
        results = dao.search(null, filter, PROJECTION, convertor);
        assertNotNull(results.getList());
        assertEquals(TYPE_LIST.size() * EXPECTED_RESULT_BY_TYPE, results.getList().size());
        testDocumentProjection(results);

        filter = new MongoSearchFilter();
        filter.setIncludedUris(URI_LIST);
        results = dao.search(null, filter, PROJECTION, convertor);
        assertNotNull(results.getList());
        assertEquals(URI_LIST.size(), results.getList().size());
        testDocumentProjection(results);
    }

    @Test
    public void searchAsStream() {
        MongoSearchFilter filter = new MongoSearchFilter().setUri(SINGLETON_URI);
        StreamWithPagination<MongoTestModel> results = dao.searchAsStream(filter);
        assertNotNull(results.getStream());
        assertEquals(1, results.getTotal());

        filter = new MongoSearchFilter();
        filter.setRdfTypes(TYPE_LIST);
        results = dao.searchAsStream(filter);
        assertNotNull(results.getStream());
        assertEquals(TYPE_LIST.size() * EXPECTED_RESULT_BY_TYPE, results.getTotal());

        filter = new MongoSearchFilter();
        filter.setIncludedUris(URI_LIST);
        results = dao.searchAsStream(filter);
        assertNotNull(results.getStream());
        assertEquals(URI_LIST.size(), results.getTotal());
    }

    @Test
    public void searchAsStreamWithSession() {
    }

    @Test
    public void searchAsStreamWithProjection() {

        MongoSearchFilter filter = new MongoSearchFilter().setUri(SINGLETON_URI);
        StreamWithPagination<MongoTestModel> results = dao.searchAsStream(null, filter, PROJECTION);
        assertNotNull(results.getStream());
        assertEquals(1, results.getTotal());
        testFieldProjection(results);

        filter = new MongoSearchFilter();
        filter.setRdfTypes(TYPE_LIST);
        results = dao.searchAsStream(null, filter, PROJECTION);
        assertNotNull(results.getStream());
        assertEquals(TYPE_LIST.size() * EXPECTED_RESULT_BY_TYPE, results.getTotal());
        testFieldProjection(results);


        filter = new MongoSearchFilter();
        filter.setIncludedUris(URI_LIST);
        results = dao.searchAsStream(null, filter, PROJECTION);
        assertNotNull(results.getStream());
        assertEquals(URI_LIST.size(), results.getTotal());
        testFieldProjection(results);
    }

    @Test
    public void distinctUris() {

    }
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
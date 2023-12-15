package org.opensilex.nosql.mongodb.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mongodb.client.ClientSession;
import com.mongodb.client.model.Projections;
import org.apache.commons.lang3.RandomStringUtils;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.*;
import org.opensilex.nosql.MongoDBServiceTest;
import org.opensilex.nosql.exceptions.NoSQLInvalidURIException;
import org.opensilex.nosql.mongodb.model.MongoTestModel;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.pagination.PaginatedIterable;
import org.opensilex.utils.pagination.StreamWithPagination;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.Assert.*;

public class MongoReadDaoTest extends MongoDBServiceTest {

    private static final Path JSON_FILE_PATH = Paths.get("src", "test", "resources", "generated_documents.zip");

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

    private static MongoReadWriteDao<MongoTestModel, MongoSearchFilter> dao;
    private static final Bson DEFAULT_PROJECTION = Projections.fields(
            Projections.include(MongoTestModel.URI_FIELD, MongoTestModel.TYPE_FIELD, MongoTestModel.NAME_FIELD, MongoTestModel.ID_FIELD)
    );

    private static final Function<MongoTestModel, Document> DEFAULT_CONVERTION = (model) -> {
        Document doc = new Document();
        doc.put(MongoTestModel.URI_FIELD, model.getUri());
        doc.put(MongoTestModel.TYPE_FIELD, model.getRdfType());
        doc.put(MongoTestModel.NAME_FIELD, model.getName());
        doc.put(MongoTestModel.ID_FIELD, model.getId());
        doc.put(MongoTestModel.VALUES_FIELD, model.getValues());
        doc.put(MongoTestModel.TAGS_FIELD, model.getTags());
        return doc;
    };

    private static final Random RANDOM = new Random();

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

    private static MongoTestModel getModel() {
        MongoTestModel model = new MongoTestModel();
        model.setId(RANDOM.nextInt());
        model.setName(RandomStringUtils.random(16));
        model.setUri(URI.create(TEST_DATASET_BASE_URI+RandomStringUtils.random(16)));
        return model;
    }

    @Test
    public void getAndExists() throws NoSQLInvalidURIException {

        // ensure exist/get return results with a known uri
        assertNotNull(dao.get(SINGLETON_URI));
        assertTrue(dao.exists(SINGLETON_URI));

        // ensure exist/get don't return results with unknown uri
        URI fakeUri = URI.create("opensilex:fake_uri");
        assertFalse(dao.exists(fakeUri));
        assertThrows(NoSQLInvalidURIException.class, () -> dao.get(fakeUri));

        // ensure null checking
        assertThrows(NullPointerException.class, () -> dao.exists(null));
        assertThrows(NullPointerException.class, () -> dao.get(null));
    }

    @Test
    public void getAndExistWithSession() {

        mongoDBServiceV2.runTransaction((session) -> {
            // Insert model inside a specific session
            MongoTestModel model = getModel();
            dao.create(model,session);

            // Should exist inside of session context
            assertNotNull(dao.get(session, model.getUri()));

            // Should not exist outside of session since transaction has not been committed
            assertThrows(NoSQLInvalidURIException.class, () -> dao.get(model.getUri()));
            assertFalse(dao.exists(model.getUri()));
        });
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

    protected void testFieldProjection(PaginatedIterable<?, MongoTestModel> results) {
        results.forEach(this::testFieldProjection);
    }

    private <T> PaginatedIterable<T> getResults(MongoSearchFilter filter, boolean useStream, boolean useProjection, boolean useConversion, ClientSession session){

        Bson projection = useProjection ? DEFAULT_PROJECTION : null;

        if(! useConversion){
            return useStream ?
                    dao.searchAsStream(session, filter, projection) :
                    dao.search(session, filter, projection);
        }else{

        }
        Function<MongoTestModel, Document> conversion = useConversion ? DEFAULT_CONVERTION : null;


    }

    private void testResultsSize(PaginatedIterable<?, MongoTestModel> results, int expectedSize){
        if(results instanceof ListWithPagination){
            assertEquals(expectedSize, ((ListWithPagination<?>) results).getList().size());
        }
        assertEquals(expectedSize, results.getPageSize());
    }

    private void testSearch(boolean useStream, boolean useProjection, boolean useConversion, boolean useSession){

        ClientSession session = null;
        try{
            session = useSession ? mongoDBServiceV2.newSession() : null;
            // filter by unique URI
            MongoSearchFilter filter = new MongoSearchFilter().setUri(SINGLETON_URI);
            PaginatedIterable<MongoTestModel> results = getResults(filter, useStream, useProjection, useConversion, session);
            assertNotNull(results);
            assertNotNull(results.getSource());
            testResultsSize(results, 1);
            if(useProjection){
                testFieldProjection(results);
            }
            if(useConversion){
                testConversion(results);
            }

            filter = new MongoSearchFilter();
            filter.setRdfTypes(TYPE_LIST);

            results = getResults(filter, useStream, useProjection, useConversion, session);
            assertNotNull(results);
            assertNotNull(results.getSource());
            testResultsSize(results, TYPE_LIST.size() * EXPECTED_RESULT_BY_TYPE);
            if(useProjection){
                testFieldProjection(results);
            }
            if(useConversion){
                testConversion(results);
            }

            filter = new MongoSearchFilter();
            filter.setIncludedUris(URI_LIST);
            results = getResults(filter, useStream, useProjection, useConversion, session);
            assertNotNull(results);
            assertNotNull(results.getSource());
            testResultsSize(results, URI_LIST.size());
            if(useProjection){
                testFieldProjection(results);
            }
            if(useConversion){
                testConversion(results);
            }


        }finally {
            if(session != null){
                session.close();
            }
        }
    }

    @Test
    public void searchWithProjection() {
    }

    protected void testConversion(PaginatedIterable<?,Document> results) {
        results.forEach(document -> {
            assertNotNull(document.get(MongoTestModel.URI_FIELD));
            assertNotNull(document.get(MongoTestModel.TYPE_FIELD));
            assertNotNull(document.get(MongoTestModel.NAME_FIELD));
            assertNotNull(document.get(MongoTestModel.ID_FIELD));
            assertNull(document.get(MongoTestModel.VALUES_FIELD));
            assertNull(document.get(MongoTestModel.TAGS_FIELD));
        });
    }

    @Test
    public void testSearch() {

        // useStream:no, useProjection:false
        testSearch(false, false, false, false);
        testSearch(false, false, false, true);
        testSearch(false, false, true, false);
        testSearch(false, false, true, true);

        // useStream:no, useProjection:true
        testSearch(false, true, false, false);
        testSearch(false, true, false, true);
        testSearch(false, true, true, false);
        testSearch(false, true, true, true);

        // useStream:true, useProjection:false
        testSearch(true, false, false, false);
        testSearch(true, false, false, true);
        testSearch(true, false, true, false);
        testSearch(true, false, true, true);

        // useStream:true, useProjection:true
        testSearch(true, true, false, false);
        testSearch(true, true, false, true);
        testSearch(true, true, true, false);
        testSearch(true, true, true, true);
    }

    @Test
    public void searchAsStream() {

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
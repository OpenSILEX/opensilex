package org.opensilex.nosql.mongodb.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mongodb.client.ClientSession;
import com.mongodb.client.model.Projections;
import org.apache.commons.lang3.RandomStringUtils;
import org.bson.conversions.Bson;
import org.junit.*;
import org.opensilex.nosql.MongoDBServiceTest;
import org.opensilex.nosql.exceptions.NoSQLInvalidURIException;
import org.opensilex.nosql.mongodb.model.MongoTestModel;
import org.opensilex.utils.pagination.PaginatedIterable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.Assert.*;
import static org.opensilex.sparql.service.SearchFilter.DEFAULT_PAGE_SIZE;

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
    private static final int ROOT_DOCUMENT_COUNT = 100;
    private static final int EXPECTED_RESULT_BY_TYPE = 10;

    private static MongoReadWriteDao<MongoTestModel, MongoSearchFilter> dao;
    private static final Bson DEFAULT_PROJECTION = Projections.fields(
            Projections.include(
                    MongoTestModel.URI_FIELD,
                    MongoTestModel.TYPE_FIELD,
                    MongoTestModel.NAME_FIELD,
                    MongoTestModel.ID_FIELD)
    );

    private static final Function<MongoTestModel, MongoTestModel> DEFAULT_CONVERSION = (model) -> {
        MongoTestModel nestedModel = new MongoTestModel();
        nestedModel.setUri(model.getUri());
        nestedModel.setRdfType(model.getRdfType());
        nestedModel.setName(model.getName());
        nestedModel.setId(model.getId());
        nestedModel.setValues(model.getValues());
        nestedModel.setTags(model.getTags());

        MongoTestModel newModel = new MongoTestModel();
        newModel.setNested(nestedModel);
        return newModel;
    };

    private static final Random RANDOM = new Random();

    @BeforeClass
    public static void setUp() {
        MongoDBServiceTest.setUp();
        dao = new MongoReadWriteDao<>(mongoDBServiceV2, MongoTestModel.class, "mongo-dao-test", "test");
        LOGGER.debug("Load json dump for testing : {} [START]", JSON_FILE_PATH);

        // Create ObjectMapper in order to parse JSON content (encoded as byte[]) to List<MongoTestModel>
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule()); // Required for MongoTestModel OffsetDateTime parsing
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
        model.setUri(URI.create(TEST_DATASET_BASE_URI + RandomStringUtils.random(16)));
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
            dao.create(model, session);

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
        assertEquals(ROOT_DOCUMENT_COUNT, dao.count(filter));

        filter.setRdfTypes(TYPE_LIST);
        assertEquals(TYPE_LIST.size() * EXPECTED_RESULT_BY_TYPE, dao.count(filter));

        filter = new MongoSearchFilter();
        filter.setIncludedUris(URI_LIST);
        assertEquals(URI_LIST.size(), dao.count(filter));
    }

    private void testSearch(
            MongoSearchFilter filter,
            boolean useStream,
            boolean useProjection,
            boolean useConversion,
            boolean useSession,
            Consumer<PaginatedIterable<MongoTestModel, ?>> resultsAssertion,
            Consumer<MongoTestModel> modelAssertion
    ) {

        Bson projection = useProjection ? DEFAULT_PROJECTION : null;
        try (ClientSession session = useSession ? mongoDBServiceV2.newSession() : null) {
            PaginatedIterable<MongoTestModel, ?> results;

            // Run search query with/without projection/conversion
            if (useConversion) {
                results = dao.search(session, filter, projection, DEFAULT_CONVERSION);
            } else {
                results = useStream ?
                        dao.searchAsStream(session, filter, projection) :
                        dao.search(session, filter, projection);
            }

            // Run assertion on results and on each item from results
            assertNotNull(results);
            resultsAssertion.accept(results);
            results.forEach(modelAssertion);
        }
    }


    private void testSearch(boolean useStream, boolean useProjection, boolean useConversion, boolean useSession) {

        Consumer<MongoTestModel> modelAssertion = (model) -> {
            if(useProjection){
                testFieldProjection(model);
            }
            if(useConversion){
                testConversion(model);
            }
        };

        // test filtering with a single URI
        MongoSearchFilter filter = new MongoSearchFilter().setUri(SINGLETON_URI);
        Consumer<PaginatedIterable<MongoTestModel,?>> resultsAssertion = (results) -> assertEquals(1, results.getTotal());
        testSearch(filter,useStream, useProjection,useConversion,useSession, resultsAssertion, modelAssertion);

        // test filtering with a type list
        filter = new MongoSearchFilter();
        filter.setRdfTypes(TYPE_LIST);
        resultsAssertion = (results) -> {
            assertEquals(DEFAULT_PAGE_SIZE, results.getPageSize());
            assertEquals(TYPE_LIST.size() * EXPECTED_RESULT_BY_TYPE, results.getTotal());
            assertEquals(0, results.getPage());
        };
        testSearch(filter,useStream, useProjection,useConversion,useSession, resultsAssertion, modelAssertion);

        // test filtering with a URI list
        filter = new MongoSearchFilter();
        filter.setIncludedUris(URI_LIST);
        resultsAssertion = (results) -> {
            assertEquals(DEFAULT_PAGE_SIZE, results.getPageSize());
            assertEquals(URI_LIST.size(), results.getTotal());
            assertEquals(0, results.getPage());
        };
        testSearch(filter,useStream, useProjection,useConversion,useSession, resultsAssertion, modelAssertion);
    }

    protected void testFieldProjection(MongoTestModel model) {
        assertNotNull(model.getUri());
        assertNotNull(model.getRdfType());
        assertNotNull(model.getName());
        assertNotNull(model.getId());
        assertNull(model.getTags());
        assertNull(model.getValues());
    }

    protected void testConversion(MongoTestModel model) {
        MongoTestModel nested = model.getNested();
        assertNotNull(nested);

        assertNotNull(nested.getUri());
        assertNotNull(nested.getRdfType());
        assertNotNull(nested.getName());
        assertNotNull(nested.getId());
    }

    @Test
    public void searchNoProjectionNoConversion() {
        testSearch(false, false, false, false);
        testSearch(false, false, false, true);
    }
    @Test
    public void searchNoProjectionConversion() {
        testSearch(false, false, true, false);
        testSearch(false, false, true, true);
    }

    @Test
    public void searchProjectionNoConversion() {
        testSearch(false, true, false, false);
        testSearch(false, true, false, true);
    }

    @Test
    public void searchProjectionConversion() {
        testSearch(false, true, true, false);
        testSearch(false, true, true, true);
    }

    @Test
    public void searchStreamNoProjectionNoConversion() {
        testSearch(true, false, false, false);
        testSearch(true, false, false, true);
    }

    @Test
    public void searchStreamNoProjectionConversion() {
        testSearch(true, false, true, false);
        testSearch(true, false, true, true);
    }

    @Test
    public void searchStreamProjectionNoConversion() {
        testSearch(true, true, false, false);
        testSearch(true, true, false, true);
    }

    @Test
    public void searchStreamProjectionConversion() {
        testSearch(true, true, true, false);
        testSearch(true, true, true, true);
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




    private void parallelSearch(ClientSession session) throws InterruptedException {

        MongoTestModel[] collectedModels = new MongoTestModel[ROOT_DOCUMENT_COUNT];
        int nbThread = 4;
        int pageSize = ROOT_DOCUMENT_COUNT/nbThread;

        // Prepare task, run 4 tasks which fetch a page from DB and insert inside array
        List<Callable<Integer>> tasks = IntStream.range(0, nbThread).mapToObj(page -> {
            MongoSearchFilter filter = new MongoSearchFilter();
            filter.setPage(page).setPageSize(pageSize);
            return new PaginatedSearchTask<>(dao, collectedModels, filter, session);
        }).collect(Collectors.toList());

        // execute all tasks in parallel , wait for task completion or timeout
        ExecutorService executor = Executors.newFixedThreadPool(nbThread);
        executor.invokeAll(tasks, 10, TimeUnit.SECONDS);

        for (MongoTestModel collectedModel : collectedModels) {
            Assert.assertNotNull(collectedModel);
        }
        Set<URI> collectedURIs = Arrays.stream(collectedModels)
                .map(MongoTestModel::getUri)
                .collect(Collectors.toSet());

        Assert.assertEquals(ROOT_DOCUMENT_COUNT, collectedURIs.size());
    }


    @Test
    public void parallelSearchTest() throws Exception {
        parallelSearch(null);
        mongoDBServiceV2.readOperationWithSession(this::parallelSearch);
    }

    @Test
    public void parallelInsertTest(){

    }

}
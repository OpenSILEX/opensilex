package org.opensilex.nosql.mongodb.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mongodb.client.ClientSession;
import com.mongodb.client.model.Projections;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertManyResult;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.conversions.Bson;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensilex.nosql.MongoDBServiceTest;
import org.opensilex.nosql.exceptions.NoSQLAlreadyExistingUriException;
import org.opensilex.nosql.exceptions.NoSQLInvalidURIException;
import org.opensilex.nosql.mongodb.model.MongoTestModel;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.pagination.PaginatedIterable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.*;
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
                    MongoTestModel.KEY_FIELD)
    );

    private static final Function<MongoTestModel, MongoTestModel> DEFAULT_CONVERSION = (model) -> {
        // this convert function read field from the model and affect these fields inside a nested model
        MongoTestModel nestedModel = new MongoTestModel();
        nestedModel.setUri(model.getUri());
        nestedModel.setRdfType(model.getRdfType());
        nestedModel.setName(model.getName());
        nestedModel.setKey(model.getKey());
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
        model.setKey(RANDOM.nextInt());
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
            // if conversion is provided, directly test the converted model
            if (useConversion) {
                testConversion(model);
            }else if (useProjection) {
                testFieldProjection(model);
            }
        };

        // test filtering with a single URI
        MongoSearchFilter filter = new MongoSearchFilter().setUri(SINGLETON_URI);
        Consumer<PaginatedIterable<MongoTestModel, ?>> resultsAssertion = (results) -> assertEquals(1, results.getTotal());
        testSearch(filter, useStream, useProjection, useConversion, useSession, resultsAssertion, modelAssertion);

        // test filtering with a type list
        filter = new MongoSearchFilter();
        filter.setRdfTypes(TYPE_LIST);
        resultsAssertion = (results) -> {
            assertEquals(DEFAULT_PAGE_SIZE, results.getPageSize());
            assertEquals(TYPE_LIST.size() * EXPECTED_RESULT_BY_TYPE, results.getTotal());
            assertEquals(0, results.getPage());
        };
        testSearch(filter, useStream, useProjection, useConversion, useSession, resultsAssertion, modelAssertion);

        // test filtering with a URI list
        filter = new MongoSearchFilter();
        filter.setIncludedUris(URI_LIST);
        resultsAssertion = (results) -> {
            assertEquals(DEFAULT_PAGE_SIZE, results.getPageSize());
            assertEquals(URI_LIST.size(), results.getTotal());
            assertEquals(0, results.getPage());
        };
        testSearch(filter, useStream, useProjection, useConversion, useSession, resultsAssertion, modelAssertion);
    }

    protected void testFieldProjection(MongoTestModel model) {
        assertNotNull(model.getUri());
        assertNotNull(model.getRdfType());
        assertNotNull(model.getName());
        assertNotNull(model.getKey());
        assertNull(model.getTags());
        assertNull(model.getValues());
    }

    protected void testConversion(MongoTestModel model) {
        MongoTestModel nested = model.getNested();
        assertNotNull(nested);

        assertNotNull(nested.getUri());
        assertNotNull(nested.getRdfType());
        assertNotNull(nested.getName());
        assertNotNull(nested.getKey());
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


    @Test
    public void testDistinctUris() {
        Set<URI> uris = dao.distinctUris(new MongoSearchFilter());
        Assert.assertEquals(ROOT_DOCUMENT_COUNT, uris.size());
        uris.forEach(Assert::assertNotNull);

        MongoSearchFilter noResultFilter = new MongoSearchFilter();
        noResultFilter.setRdfTypes(List.of(URI.create("test:unknown_type")));
        Set<URI> noUris = dao.distinctUris(noResultFilter);
        Assert.assertTrue(noUris.isEmpty());
    }

    @Test
    public void testDistinct() {
        // extract distinct types (no pagination here)
        Set<String> names = dao.distinct(MongoTestModel.NAME_FIELD, String.class, new MongoSearchFilter(), null);
        Assert.assertEquals(DEFAULT_PAGE_SIZE, names.size());
        names.forEach(name -> Assert.assertFalse(StringUtils.isEmpty(name)));

        // no results -> ensure non nullity of Set
        MongoSearchFilter noResultFilter = new MongoSearchFilter();
        noResultFilter.setRdfTypes(List.of(URI.create("test:unknown_type")));
        Set<String> noNames = dao.distinct(MongoTestModel.NAME_FIELD, String.class, noResultFilter, null);
        Assert.assertTrue(noNames.isEmpty());
    }

    @Test
    public void testDistinctAggregation() {

        // extract distinct types (pagination enabled : use aggregation pipeline)
        Set<String> names = dao.distinctAggregation(MongoTestModel.NAME_FIELD, String.class, new MongoSearchFilter(), null);
        Assert.assertEquals(DEFAULT_PAGE_SIZE, names.size());
        names.forEach(name -> Assert.assertFalse(StringUtils.isEmpty(name)));

        // no results -> ensure non nullity of Set
        MongoSearchFilter noResultFilter = new MongoSearchFilter();
        noResultFilter.setRdfTypes(List.of(URI.create("test:unknown_type")));
        Set<String> noNames = dao.distinctAggregation(MongoTestModel.NAME_FIELD, String.class, noResultFilter, null);
        Assert.assertTrue(noNames.isEmpty());
    }


    private void parallelSearch(ClientSession session) throws InterruptedException {

        MongoTestModel[] collectedModels = new MongoTestModel[ROOT_DOCUMENT_COUNT];
        int nbThread = 4;
        int pageSize = ROOT_DOCUMENT_COUNT / nbThread;

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

    public void parallelInsertTest(boolean generateSession) throws InterruptedException, ExecutionException {

        var innerDao = new MongoReadWriteDao<>(mongoDBServiceV2, MongoTestModel.class, "mongo-dao-write-test", "test");
        int nbThread = 4;
        int nbModelPerThread = 100;

        // Prepare tasks
        List<Callable<InsertManyResult>> tasks = IntStream.range(0, nbThread).mapToObj(threadIdx -> {

            // Task : Create models to insert
            List<MongoTestModel> models = IntStream.range(0, nbModelPerThread).mapToObj(modelIdx -> {
                MongoTestModel model = new MongoTestModel();
                model.setName(threadIdx + "_" + threadIdx);
                return model;
            }).collect(Collectors.toList());

            return new MongoInsertTask<>(mongoDBServiceV2, innerDao, models, generateSession);

        }).collect(Collectors.toList());

        // execute all tasks in parallel , wait for task completion or timeout
        ExecutorService executor = Executors.newFixedThreadPool(nbThread);
        var insertResults = executor.invokeAll(tasks, 10, TimeUnit.SECONDS);

        for (Future<InsertManyResult> taskResult : insertResults) {
            InsertManyResult insertResult = taskResult.get();
            Assert.assertEquals(nbModelPerThread, insertResult.getInsertedIds().size());
        }

        // Run search and ensure results are OK
        MongoSearchFilter searchFilter = new MongoSearchFilter();
        searchFilter.setPage(nbModelPerThread * nbThread);

        ListWithPagination<MongoTestModel> searchResults = innerDao.search(searchFilter);
        Assert.assertEquals(nbModelPerThread * nbThread, searchResults.getTotal());
        searchResults.forEach(model -> {
            Assert.assertNotNull(model.getUri());
            Assert.assertNotNull(model.getName());
        });

        // Delete objects
        innerDao.deleteMany(new MongoSearchFilter());
    }

    @Test
    public void parallelInsertTest() throws InterruptedException, ExecutionException {
        parallelInsertTest(false);
        parallelInsertTest(true);
    }

    @Test
    public void createTest() throws NoSQLAlreadyExistingUriException, URISyntaxException {
        var innerDao = new MongoReadWriteDao<>(mongoDBServiceV2, MongoTestModel.class, "mongo-dao-create-test", "test");
        MongoTestModel model = new MongoTestModel();
        model.setName("create");
        innerDao.create(model);
        assertNotNull(model.getUri());
        assertEquals(1, innerDao.count(new MongoSearchFilter()));
    }


    @Test
    public void createAllTest() throws NoSQLAlreadyExistingUriException, URISyntaxException {
        var innerDao = new MongoReadWriteDao<>(mongoDBServiceV2, MongoTestModel.class, "mongo-dao-create-all-test", "test");
        MongoTestModel model = new MongoTestModel();
        model.setName("create");
        MongoTestModel model2 = new MongoTestModel();
        model2.setName("create2");

        innerDao.create(List.of(model, model2));
        assertNotNull(model.getUri());
        assertNotNull(model2.getUri());
        assertEquals(2, innerDao.count(new MongoSearchFilter()));

        innerDao.deleteMany(new MongoSearchFilter());
    }
    @Test
    public void updateTest() throws NoSQLAlreadyExistingUriException, URISyntaxException, NoSQLInvalidURIException {

        var innerDao = new MongoReadWriteDao<>(mongoDBServiceV2, MongoTestModel.class, "mongo-dao-update-test", "test");
        MongoTestModel model = new MongoTestModel();
        model.setName("update_1");
        innerDao.create(model);

        model.setName("update_2");
        model.setRdfType(URI.create("test:updated_type"));
        innerDao.update(model);

        MongoTestModel modelFromDB = innerDao.get(model.getUri());
        Assert.assertEquals(modelFromDB.getName(), model.getName());
        Assert.assertEquals(modelFromDB.getRdfType(), model.getRdfType());

        MongoTestModel unknown = new MongoTestModel();
        unknown.setUri(URI.create("test:fake_uri"));
        assertThrows(NoSQLInvalidURIException.class, () -> innerDao.update(unknown));

        innerDao.deleteMany(new MongoSearchFilter());
    }


    @Test
    public void deleteTest() throws NoSQLAlreadyExistingUriException, URISyntaxException, NoSQLInvalidURIException {

        var innerDao = new MongoReadWriteDao<>(mongoDBServiceV2, MongoTestModel.class, "mongo-dao-update-test", "test");
        MongoTestModel model = new MongoTestModel();
        model.setName("update_1");
        innerDao.create(model);

        Assert.assertNotNull(innerDao.get(model.getUri()));
        innerDao.delete(model.getUri());
        assertThrows(NoSQLInvalidURIException.class, () -> innerDao.get(model.getUri()));

        innerDao.deleteMany(new MongoSearchFilter());
    }

    @Test
    public void deleteManyTest() throws NoSQLAlreadyExistingUriException, URISyntaxException {

        var innerDao = new MongoReadWriteDao<>(mongoDBServiceV2, MongoTestModel.class, "mongo-dao-delete-test", "test");

        // create multiple model with several types
        URI type1 = URI.create("test:type1");
        URI type2 = URI.create("test:type2");
        int nbModelByType = 10;

        List<MongoTestModel> modelsWithType1 = IntStream.range(0, nbModelByType).mapToObj(i -> {
            MongoTestModel model = new MongoTestModel();
            model.setRdfType(type1);
            model.setName("delete_1");
            return model;
        }).collect(Collectors.toList());
        innerDao.create(modelsWithType1);

        List<MongoTestModel> modelsWithType2 = IntStream.range(0, nbModelByType).mapToObj(i -> {
            MongoTestModel model = new MongoTestModel();
            model.setRdfType(type2);
            model.setName("delete_1");
            return model;
        }).collect(Collectors.toList());
        innerDao.create(modelsWithType2);

        Assert.assertEquals(nbModelByType*2, innerDao.count(new MongoSearchFilter()));

        // delete all document
        MongoSearchFilter filter = new MongoSearchFilter();
        filter.setRdfTypes(List.of(type1));
        DeleteResult deleteResult = innerDao.deleteMany(filter);
        Assert.assertEquals(nbModelByType, deleteResult.getDeletedCount());
        Assert.assertEquals(nbModelByType, innerDao.count(new MongoSearchFilter()));
    }

}
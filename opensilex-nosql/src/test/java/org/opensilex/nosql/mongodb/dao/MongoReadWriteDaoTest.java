package org.opensilex.nosql.mongodb.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mongodb.client.ClientSession;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Projections;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertManyResult;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.arq.querybuilder.Order;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.bson.conversions.Bson;
import org.eclipse.rdf4j.repository.Repository;
import org.jetbrains.annotations.NotNull;
import org.junit.*;
import org.opensilex.nosql.MongoDBServiceTest;
import org.opensilex.nosql.distributed.SparqlMongoTransaction;
import org.opensilex.nosql.exceptions.MongoDbUniqueIndexConstraintViolation;
import org.opensilex.nosql.exceptions.NoSQLAlreadyExistingUriException;
import org.opensilex.nosql.exceptions.NoSQLInvalidURIException;
import org.opensilex.nosql.mongodb.model.MongoTestModel;
import org.opensilex.nosql.mongodb.model.SparqlMongoTestModel;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.sparql.exceptions.SPARQLInvalidClassDefinitionException;
import org.opensilex.sparql.rdf4j.RDF4JConnection;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.service.SPARQLServiceFactory;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;
import org.opensilex.utils.pagination.PaginatedIterable;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
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
import static org.junit.Assert.assertEquals;
import static org.opensilex.sparql.service.SearchFilter.DEFAULT_PAGE_SIZE;

import org.opensilex.utils.pagination.PaginatedSearchStrategy;
import org.opensilex.utils.pagination.StreamWithPagination;

public class MongoReadWriteDaoTest extends MongoDBServiceTest {

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

    // Use this DAO inside test just for searching document from the JSON dump
    private static MongoReadWriteDao<MongoTestModel, MongoSearchFilter> searchDao;

    // Other DAO used just for write other read/write tests
    private static MongoReadWriteDao<MongoTestModel, MongoSearchFilter> readWriteDao;

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
    public static void setUp()  {
        MongoDBServiceTest.setUp();
        searchDao = new MongoReadWriteDao<>(mongoDBv2, MongoTestModel.class, "mongo-dao-search-test", "test");
        readWriteDao = new MongoReadWriteDao<>(mongoDBv2, MongoTestModel.class, "mongo-read-write-test", "test");
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
            mongoDBv2.runTransaction(session -> searchDao.getCollection().insertMany(session, models));

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        // Load data from json file
        long durationMs = Duration.between(start, Instant.now()).toMillis();
        LOGGER.debug("Load json dump for testing : {} [OK] duration: {} ms", JSON_FILE_PATH, durationMs);

        SPARQLServiceFactory factory = getOpensilex().getServiceInstance(SPARQLService.DEFAULT_SPARQL_SERVICE, SPARQLServiceFactory.class);

        try {
            factory.getMapperIndex().addClasses(SparqlMongoTestModel.class);
        } catch (SPARQLInvalidClassDefinitionException e) {
            throw new RuntimeException(e);
        }
    }

    @AfterClass
    public static void stop() {
        searchDao.getCollection().drop();
        MongoDBServiceTest.stop();
    }

    @After
    public void afterEach() {
        readWriteDao.getCollection().drop();
        Assert.assertEquals(0, readWriteDao.count(new MongoSearchFilter()));
    }

    private static MongoTestModel getModel() {
        MongoTestModel model = new MongoTestModel();
        model.setKey(RANDOM.nextInt());
        model.setName(RandomStringUtils.random(16));
        model.setUri(URI.create(TEST_DATASET_BASE_URI + RandomStringUtils.randomAlphanumeric(16)));
        return model;
    }

    @Test
    public void getAndExists() throws NoSQLInvalidURIException {

        // ensure exist/get return results with a known uri
        assertNotNull(searchDao.get(SINGLETON_URI));
        assertTrue(searchDao.exists(SINGLETON_URI));

        // ensure exist/get don't return results with unknown uri
        URI fakeUri = URI.create("opensilex:fake_uri");
        assertFalse(searchDao.exists(fakeUri));
        assertThrows(NoSQLInvalidURIException.class, () -> searchDao.get(fakeUri));

        // ensure null checking
        assertThrows(NullPointerException.class, () -> searchDao.exists(null));
        assertThrows(NullPointerException.class, () -> searchDao.get(null));
    }

    @Test
    public void getAndExistWithSession() throws Exception {

        mongoDBv2.runThrowingTransaction((session) -> {
            // Insert model inside a specific session
            MongoTestModel model = getModel();
            searchDao.create(session, model);

            // Should exist inside of session context
            assertNotNull(searchDao.get(session, model.getUri()));

            // Should not exist outside of session since transaction has not been committed
            assertThrows(NoSQLInvalidURIException.class, () -> searchDao.get(model.getUri()));
            assertFalse(searchDao.exists(model.getUri()));
        });
    }

    @Test
    public void count() {
        MongoSearchFilter filter = new MongoSearchFilter().setUri(SINGLETON_URI);
        assertEquals(1, searchDao.count(filter));

        filter = new MongoSearchFilter();
        assertEquals(ROOT_DOCUMENT_COUNT, searchDao.count(filter));

        filter.setRdfTypes(TYPE_LIST);
        assertEquals(TYPE_LIST.size() * EXPECTED_RESULT_BY_TYPE, searchDao.count(filter));

        filter = new MongoSearchFilter();
        filter.setIncludedUris(URI_LIST);
        assertEquals(URI_LIST.size(), searchDao.count(filter));
    }

    private void testSearch(
            MongoSearchFilter filter,
            boolean useStream,
            boolean useProjection,
            boolean useConversion,
            boolean useSession,
            PaginatedSearchStrategy strategy,
            Consumer<PaginatedIterable<MongoTestModel, ?>> resultsAssertion,
            Consumer<MongoTestModel> modelAssertion,
            Consumer<PaginatedListResponse<MongoTestModel>> paginatedResponseAssertion
            ) {

        try (ClientSession session = useSession ? mongoDBv2.startSession() : null) {
            PaginatedIterable<MongoTestModel, ?> results;

            MongoSearchQuery<MongoTestModel, MongoSearchFilter, MongoTestModel> query = new MongoSearchQuery<>();
            query.setSession(session);
            query.setFilter(filter);
            if(useProjection){
                query.setProjection(DEFAULT_PROJECTION);
            }
            query.setConvertFunction(useConversion ? DEFAULT_CONVERSION : Function.identity());
            query.setPaginationStrategy(strategy);

            // Run search query with/without projection/conversion
            results = useStream ?
                    searchDao.searchAsStreamWithPagination(query) :
                    searchDao.searchWithPagination(query);

            // Run assertion on results and on each item from results
            assertNotNull(results);
            resultsAssertion.accept(results);

            PaginatedListResponse<MongoTestModel> paginatedResponse;
            if(useStream){
                paginatedResponse = new PaginatedListResponse<>((StreamWithPagination<MongoTestModel>) results);
            }else {
                paginatedResponse = new PaginatedListResponse<>((ListWithPagination<MongoTestModel>) results);
            }
            paginatedResponse.getResult().forEach(modelAssertion);
            paginatedResponseAssertion.accept(paginatedResponse);
        }
    }

    private  Consumer<PaginatedIterable<MongoTestModel, ?>> getResultsAssertion(int expectedSize,  PaginatedSearchStrategy strategy){
        // Assertion on Database results
        return (results) -> {
            assertEquals(DEFAULT_PAGE_SIZE, results.getPageSize());
            assertEquals(0, results.getPage());

            if(strategy == PaginatedSearchStrategy.COUNT_QUERY_BEFORE_SEARCH){
                assertEquals(expectedSize, results.getTotal());
            }else if(strategy == PaginatedSearchStrategy.HAS_NEXT_PAGE){
                assertEquals(0, results.getTotal());
            }
        };
    }

    private Consumer<PaginatedListResponse<MongoTestModel>> getPaginatedResponseAssertion(int expectedSize,  PaginatedSearchStrategy strategy){

        // // Assertion on Paginated response
        return (response) -> {

            // check the number of element effectively returned
            assertEquals(Math.min(expectedSize, DEFAULT_PAGE_SIZE), response.getResult().size());

            // check pagination from metadata
            assertEquals(DEFAULT_PAGE_SIZE, response.getMetadata().getPagination().getPageSize());
            assertEquals(0, response.getMetadata().getPagination().getCurrentPage());

            if(strategy == PaginatedSearchStrategy.COUNT_QUERY_BEFORE_SEARCH){
                assertEquals(expectedSize, response.getMetadata().getPagination().getTotalCount());
            }else if(strategy == PaginatedSearchStrategy.HAS_NEXT_PAGE){
                assertEquals(0, response.getMetadata().getPagination().getTotalCount());
            }
        };
    }

    private void testSearch(boolean useStream, boolean useProjection, boolean useConversion, boolean useSession) {
        this.testSearch(useStream,useProjection,useConversion,useSession, PaginatedSearchStrategy.COUNT_QUERY_BEFORE_SEARCH);
    }

    private void testSearch(boolean useStream, boolean useProjection, boolean useConversion, boolean useSession, PaginatedSearchStrategy strategy) {

        Consumer<MongoTestModel> modelAssertion = (model) -> {
            // if conversion is provided, directly test the converted model
            if (useConversion) {
                testConversion(model);
            } else if (useProjection) {
                testFieldProjection(model);
            }
        };

        // test filtering with a single URI
        MongoSearchFilter filter = new MongoSearchFilter().setUri(SINGLETON_URI);
        int expectedSize = 1;
        testSearch(
                filter,
                useStream,
                useProjection,
                useConversion,
                useSession,
                strategy,
                getResultsAssertion(expectedSize, strategy),
                modelAssertion,
                getPaginatedResponseAssertion(expectedSize, strategy)
        );

        // test filtering with a type list
        filter = new MongoSearchFilter();
        filter.setRdfTypes(TYPE_LIST);
        expectedSize = TYPE_LIST.size() * EXPECTED_RESULT_BY_TYPE;

        testSearch(
                filter,
                useStream,
                useProjection,
                useConversion,
                useSession,
                strategy,
                getResultsAssertion(expectedSize, strategy),
                modelAssertion,
                getPaginatedResponseAssertion(expectedSize, strategy)
        );

        // test filtering with a URI list
        filter = new MongoSearchFilter();
        filter.setIncludedUris(URI_LIST);
        expectedSize = URI_LIST.size();
        testSearch(
                filter,
                useStream,
                useProjection,
                useConversion,
                useSession,
                strategy,
                getResultsAssertion(expectedSize, strategy),
                modelAssertion,
                getPaginatedResponseAssertion(expectedSize, strategy)
        );
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
        testSearch(false, false, false, false, PaginatedSearchStrategy.HAS_NEXT_PAGE);
        testSearch(false, false, false, true, PaginatedSearchStrategy.HAS_NEXT_PAGE);
    }

    @Test
    public void searchNoProjectionConversion() {
        testSearch(false, false, true, false);
        testSearch(false, false, true, true);
        testSearch(false, false, true, false, PaginatedSearchStrategy.HAS_NEXT_PAGE);
        testSearch(false, false, true, true, PaginatedSearchStrategy.HAS_NEXT_PAGE);
    }

    @Test
    public void searchProjectionNoConversion() {
        testSearch(false, true, false, false);
        testSearch(false, true, false, true);
        testSearch(false, true, false, false, PaginatedSearchStrategy.HAS_NEXT_PAGE);
        testSearch(false, true, false, true, PaginatedSearchStrategy.HAS_NEXT_PAGE);
    }

    @Test
    public void searchProjectionConversion() {
        testSearch(false, true, true, false);
        testSearch(false, true, true, true);
        testSearch(false, true, true, false, PaginatedSearchStrategy.HAS_NEXT_PAGE);
        testSearch(false, true, true, true, PaginatedSearchStrategy.HAS_NEXT_PAGE);
    }

    @Test
    public void searchStreamNoProjectionNoConversion() {
        testSearch(true, false, false, false);
        testSearch(true, false, false, true);
        testSearch(true, false, false, false, PaginatedSearchStrategy.HAS_NEXT_PAGE);
        testSearch(true, false, false, true, PaginatedSearchStrategy.HAS_NEXT_PAGE);
    }

    @Test
    public void searchStreamNoProjectionConversion() {
        testSearch(true, false, true, false);
        testSearch(true, false, true, true);
        testSearch(true, false, true, false, PaginatedSearchStrategy.HAS_NEXT_PAGE);
        testSearch(true, false, true, true, PaginatedSearchStrategy.HAS_NEXT_PAGE);
    }

    @Test
    public void searchStreamProjectionNoConversion() {
        testSearch(true, true, false, false);
        testSearch(true, true, false, true);
        testSearch(true, true, false, false, PaginatedSearchStrategy.HAS_NEXT_PAGE);
        testSearch(true, true, false, true, PaginatedSearchStrategy.HAS_NEXT_PAGE);
    }

    @Test
    public void searchStreamProjectionConversion() {
//        testSearch(true, true, true, false);
//        testSearch(true, true, true, true);
        testSearch(true, true, true, false, PaginatedSearchStrategy.HAS_NEXT_PAGE);
        testSearch(true, true, true, true, PaginatedSearchStrategy.HAS_NEXT_PAGE);
    }


    @Test
    public void testDistinctUris() {
        List<URI> uris = searchDao.distinctUris(new MongoSearchFilter());
        Assert.assertEquals(ROOT_DOCUMENT_COUNT, uris.size());
        uris.forEach(Assert::assertNotNull);

        MongoSearchFilter noResultFilter = new MongoSearchFilter();
        noResultFilter.setRdfTypes(List.of(URI.create("test:unknown_type")));
        List<URI> noUris = searchDao.distinctUris(noResultFilter);
        Assert.assertTrue(noUris.isEmpty());
    }

    @Test
    public void testDistinct() throws NoSQLAlreadyExistingUriException, URISyntaxException {

        int nbModel = 20;
        // create models, with nbModel/2 different names
        List<MongoTestModel> models = new ArrayList<>(nbModel);
        for (int i = 0; i < nbModel / 2; i++) {
            MongoTestModel model1 = new MongoTestModel();
            model1.setName("name_" + i);
            MongoTestModel model2 = new MongoTestModel();
            model2.setName("name_" + i);
            models.add(model1);
            models.add(model2);
        }
        readWriteDao.create(models);

        // extract distinct types (no pagination here)
        Collection<String> distinctNames = readWriteDao.distinct(null, MongoTestModel.NAME_FIELD, String.class, new MongoSearchFilter());
        Assert.assertEquals(nbModel / 2, distinctNames.size());
        distinctNames.forEach(name -> Assert.assertFalse(StringUtils.isEmpty(name)));

        // extract distinct types (pagination with aggregation pipeline)
        MongoSearchFilter filter = new MongoSearchFilter();
        filter.setOrderByList(List.of(new OrderBy("name", Order.ASCENDING)));
        filter.setPageSize(5);
        ListWithPagination<String> distinctNamesPaginated = readWriteDao.distinctWithPagination(null, MongoTestModel.NAME_FIELD, String.class, filter);
        Assert.assertEquals(5, distinctNamesPaginated.getList().size());

        distinctNamesPaginated.forEach(name -> Assert.assertFalse(StringUtils.isEmpty(name)));

        // no results -> ensure non nullity of Set
        MongoSearchFilter noResultFilter = new MongoSearchFilter();
        noResultFilter.setRdfTypes(List.of(URI.create("test:unknown_type")));
        Collection<String> noNames = searchDao.distinct(null, MongoTestModel.NAME_FIELD, String.class, noResultFilter);
        Assert.assertTrue(noNames.isEmpty());

        // no results -> ensure non nullity of Set (with aggregation pipeline)
        ListWithPagination<String> noNamesPaginated = searchDao.distinctWithPagination(null, MongoTestModel.NAME_FIELD, String.class, noResultFilter);
        Assert.assertTrue(noNamesPaginated.getList().isEmpty());
    }

    private void parallelSearch(ClientSession session) throws InterruptedException {

        MongoTestModel[] collectedModels = new MongoTestModel[ROOT_DOCUMENT_COUNT];
        int nbThread = 4;
        int pageSize = ROOT_DOCUMENT_COUNT / nbThread;

        // Prepare task, run 4 tasks which fetch a page from DB and insert inside array
        List<Callable<Integer>> tasks = IntStream.range(0, nbThread).mapToObj(page -> {
            MongoSearchFilter filter = new MongoSearchFilter();
            filter.setPage(page).setPageSize(pageSize);
            return new PaginatedSearchTask<>(searchDao, collectedModels, filter, session);
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
        mongoDBv2.withSession(this::parallelSearch);
    }

    public void parallelInsertTest(boolean generateSession) throws InterruptedException {

        int nbThread = 4;
        int nbModelPerThread = 512;

        ExecutorService executor = Executors.newFixedThreadPool(nbThread);
        // Prepare tasks
        for (int threadIdx = 0; threadIdx < nbThread; threadIdx++) {

            URI type = URI.create("test:parallel_write_type_" + threadIdx);
            // Task : Create models to insert
            List<MongoTestModel> models = new ArrayList<>(nbModelPerThread);
            for (int modelIdx = 0; modelIdx < nbModelPerThread; modelIdx++) {
                MongoTestModel model = new MongoTestModel();
                model.setName(threadIdx + "_" + modelIdx);
                model.setRdfType(type);
                model.setKey(threadIdx * modelIdx);
                model.setUri(URI.create("test:" + model.getName() + "_" + UUID.randomUUID()));
                models.add(model);
            }

            // Generate a task
            Callable<InsertManyResult> task = generateSession ?
                    () -> mongoDBv2.computeThrowingTransaction((session) -> readWriteDao.create(session, models)) :
                    () -> readWriteDao.create(models);
            executor.submit(task);
        }

        // Run search and ensure results are OK
        Thread.sleep(2000);
        MongoSearchFilter searchFilter = new MongoSearchFilter();
        searchFilter.setPageSize(nbModelPerThread * nbThread);

        ListWithPagination<MongoTestModel> searchResults = readWriteDao.searchWithPagination(searchFilter);
        Assert.assertEquals(nbModelPerThread * nbThread, searchResults.getTotal());
        Assert.assertEquals(nbModelPerThread * nbThread, searchResults.getSource().size());

        searchResults.forEach(model -> {
            Assert.assertNotNull(model.getUri());
            Assert.assertNotNull(model.getName());
        });

        // Delete objects
        executor.shutdown();
    }

    @Test
    public void parallelInsertTest() throws InterruptedException {
        parallelInsertTest(false);
        readWriteDao.getCollection().drop();
        parallelInsertTest(true);
    }

    @Test
    public void createTest() throws NoSQLAlreadyExistingUriException, URISyntaxException {
        MongoTestModel model = new MongoTestModel();
        model.setName("create");
        readWriteDao.create(model);
        assertNotNull(model.getUri());
        assertEquals(1, readWriteDao.count(new MongoSearchFilter()));
    }


    @Test
    public void createAllTest() throws NoSQLAlreadyExistingUriException, URISyntaxException {
        MongoTestModel model = new MongoTestModel();
        model.setName("create");
        MongoTestModel model2 = new MongoTestModel();
        model2.setName("create2");

        readWriteDao.create(List.of(model, model2));
        assertNotNull(model.getUri());
        assertNotNull(model2.getUri());
        assertEquals(2, readWriteDao.count(new MongoSearchFilter()));

    }

    @Test
    public void createAllFailTest() throws NoSQLAlreadyExistingUriException, URISyntaxException {

        // create an index by name
        mongoDBv2.createIndex(
                readWriteDao.getCollection(),
                Indexes.ascending(MongoTestModel.NAME_FIELD),
                new IndexOptions().unique(true)
        );

        MongoTestModel model = new MongoTestModel();
        model.setName("create");
        MongoTestModel model2 = new MongoTestModel();
        model2.setName("create");
        MongoTestModel model3 = new MongoTestModel();
        model3.setName("create");

        // Trigger the throws on a MongoException due to conflict on field name due to the unique name index
        List<MongoTestModel> models = List.of(model, model2, model3);
        Assert.assertThrows(MongoDbUniqueIndexConstraintViolation.class, () -> readWriteDao.create(models));
        Assert.assertThrows(MongoDbUniqueIndexConstraintViolation.class, () -> mongoDBv2.runThrowingTransaction((session) -> readWriteDao.create(session, models)));

        // Trigger error to invalid list
        Assert.assertThrows(IllegalArgumentException.class, () -> mongoDBv2.runThrowingTransaction((session) -> readWriteDao.create(session, Collections.emptyList())));

        // create the first model and then try to create the 2nd one, also expect MongoDbUniqueIndexConstraintViolation
        readWriteDao.create(model);
        Assert.assertThrows(MongoDbUniqueIndexConstraintViolation.class, () -> readWriteDao.create(model2));
        Assert.assertThrows(MongoDbUniqueIndexConstraintViolation.class, () -> mongoDBv2.runThrowingTransaction((session) -> readWriteDao.create(session, model2)));
    }


    @Test
    public void updateTest() throws NoSQLAlreadyExistingUriException, URISyntaxException, NoSQLInvalidURIException {

        MongoTestModel model = new MongoTestModel();
        model.setName("update_1");
        readWriteDao.create(model);

        model.setName("update_2");
        model.setRdfType(URI.create("test:updated_type"));
        readWriteDao.update(model);

        MongoTestModel modelFromDB = readWriteDao.get(model.getUri());
        Assert.assertEquals(modelFromDB.getName(), model.getName());
        Assert.assertEquals(modelFromDB.getRdfType(), model.getRdfType());

        MongoTestModel unknown = new MongoTestModel();
        unknown.setUri(URI.create("test:fake_uri"));
        assertThrows(NoSQLInvalidURIException.class, () -> readWriteDao.update(unknown));

    }


    @Test
    public void deleteTest() throws NoSQLAlreadyExistingUriException, URISyntaxException, NoSQLInvalidURIException {

        MongoTestModel model = new MongoTestModel();
        model.setName("update_1");
        readWriteDao.create(model);

        Assert.assertNotNull(readWriteDao.get(model.getUri()));
        readWriteDao.delete(model.getUri());
        assertThrows(NoSQLInvalidURIException.class, () -> readWriteDao.get(model.getUri()));

    }

    @Test
    public void deleteManyTest() throws NoSQLAlreadyExistingUriException, URISyntaxException {

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
        readWriteDao.create(modelsWithType1);

        List<MongoTestModel> modelsWithType2 = IntStream.range(0, nbModelByType).mapToObj(i -> {
            MongoTestModel model = new MongoTestModel();
            model.setRdfType(type2);
            model.setName("delete_1");
            return model;
        }).collect(Collectors.toList());
        readWriteDao.create(modelsWithType2);

        Assert.assertEquals(nbModelByType * 2, readWriteDao.count(new MongoSearchFilter()));

        // delete all document
        MongoSearchFilter filter = new MongoSearchFilter();
        filter.setRdfTypes(List.of(type1));
        DeleteResult deleteResult = readWriteDao.deleteMany(filter);
        Assert.assertEquals(nbModelByType, deleteResult.getDeletedCount());
        Assert.assertEquals(nbModelByType, readWriteDao.count(new MongoSearchFilter()));
    }

    @Test
    public void testDeleteManyWithEmptyFilterFail(){
        Assert.assertThrows(IllegalArgumentException.class, () -> readWriteDao.deleteMany(MongoSearchFilter.EMPTY));
    }

    @Test
    public void testSparqlMongoTransaction() throws Exception {

        int nbModel = 100;

        SparqlMongoTransaction trx = new SparqlMongoTransaction(sparql, mongoDBv2);
        Node graph = NodeFactory.createURI("test:sparql-mongo-test");

        Assert.assertEquals(0, sparql.count(SparqlMongoTestModel.class));
        Assert.assertEquals(0, readWriteDao.count(MongoSearchFilter.EMPTY));

        var result = trx.execute((mongoSession) -> {

            List<SparqlMongoTestModel> sparqlModels = new ArrayList<>(nbModel);
            List<MongoTestModel> mongoModels = new ArrayList<>(nbModel);

            for (int i = 0; i < nbModel; i++) {
                SparqlMongoTestModel sparqlModel = new SparqlMongoTestModel();
                sparqlModel.setUri(URI.create("testSparqlMongoTransaction:" + i));
                sparqlModels.add(sparqlModel);

                MongoTestModel mongoModel = new MongoTestModel();
                mongoModel.setName("test" + i);
                mongoModels.add(mongoModel);
            }

            sparql.create(graph, sparqlModels);
            readWriteDao.create(mongoSession, mongoModels);

            return Pair.of(sparqlModels, mongoModels);
        });

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getLeft());
        Assert.assertNotNull(result.getRight());

        Assert.assertEquals(nbModel, sparql.count(SparqlMongoTestModel.class));
        Assert.assertEquals(nbModel, readWriteDao.count(MongoSearchFilter.EMPTY));

        sparql.clearGraph(graph.toString());
    }

    @Test
    public void testParallelSparqlMongoTransaction() throws Exception {
        int nbModelPerThread = 100;
        int nbThread = 1;
        Node graph = NodeFactory.createURI("test:sparql-mongo-test");

        // Prepare tasks
        ExecutorService executor = Executors.newFixedThreadPool(nbThread);
        Repository sparqlRepository = sparql.getRepositoryConnection().getRepository();

        for (int threadIdx = 0; threadIdx < nbThread; threadIdx++) {
            List<SparqlMongoTestModel> sparqlModels = new ArrayList<>(nbModelPerThread);
            List<MongoTestModel> mongoModels = new ArrayList<>(nbModelPerThread);

            // Thread-safety for SPARQL is guaranteed at RepositoryConnection level. Each SPARQLService use a dedicated RepositoryConnection
            // Here for testing purpose, we reuse the Repository from the SPARQLService initialized for test
            SPARQLService newSparqlService = getNewSparqlService(sparqlRepository);

            for (int modelIdx = 0; modelIdx < nbModelPerThread; modelIdx++) {
                SparqlMongoTestModel sparqlModel = new SparqlMongoTestModel();
                sparqlModel.setUri(URI.create("test:" + threadIdx + "_" + UUID.randomUUID()));
                sparqlModels.add(sparqlModel);

                MongoTestModel mongoModel = new MongoTestModel();
                mongoModel.setName("test" + modelIdx + "_" + threadIdx);
                mongoModels.add(mongoModel);
            }

            var transaction = new SparqlMongoTransaction(newSparqlService, mongoDBv2);
            Callable<InsertManyResult> task = () -> transaction.execute((mongoSession) -> {
                newSparqlService.createWithoutTransaction(graph, sparqlModels, 1000, false, false);
                return readWriteDao.create(mongoSession, mongoModels);
            });
            executor.submit(task);
        }

        // Run search and ensure results are OK
        Thread.sleep(2000);
        Assert.assertEquals(nbModelPerThread * nbThread, sparql.count(SparqlMongoTestModel.class));
        Assert.assertEquals(nbModelPerThread * nbThread, readWriteDao.count(MongoSearchFilter.EMPTY));
        executor.shutdown();
    }

    @NotNull
    private static SPARQLService getNewSparqlService(Repository sparqlRepository) throws Exception {
        SPARQLService newSparqlConnection = new SPARQLService(new RDF4JConnection(sparqlRepository.getConnection()));
        newSparqlConnection.setOpenSilex(getOpensilex());
        newSparqlConnection.setMapperIndex(sparql.getMapperIndex());
        newSparqlConnection.setDefaultLang(sparql.getDefaultLang());
        newSparqlConnection.setup();
        return newSparqlConnection;
    }

}